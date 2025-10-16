terraform {
  required_version = ">= 1.0.0" # Ensure that the Terraform version is 1.0.0 or higher

  required_providers {
    aws = {
      source  = "hashicorp/aws" # Specify the source of the AWS provider
      version = "~> 5.49"      # Use a version of the AWS provider that is compatible with version
    }
  }
}

provider "aws" {
  region = "us-east-1" # Set the AWS region to US East (N. Virginia)
}

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags = {
    Name = "main-vpc"
  }
}

# Internet Gateway
resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.main.id
  tags = {
    Name = "main-igw"
  }
}

# Public Subnet
resource "aws_subnet" "public_zone1" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.0.0/19"
  availability_zone = "us-east-1a"
  map_public_ip_on_launch = true # Instances in this subnet should get a public IP
  tags = {
    Name = "public-subnet-us-east-1a"
  }
}

# Elastic IP for NAT Gateway
resource "aws_eip" "nat_eip" { # FIX 1: Declared aws_eip resource for NAT Gateway
  vpc = true
  tags = {
    Name = "nat-eip"
  }
}

# NAT Gateway
resource "aws_nat_gateway" "nat" {
  allocation_id = aws_eip.nat_eip.id # FIX 1: Corrected reference to the EIP
  subnet_id     = aws_subnet.public_zone1.id
  tags = {
    Name = "main-nat-gateway"
  }
  depends_on = [aws_internet_gateway.igw] # Ensure IGW is ready before NAT GW
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }

}
resource "aws_route_table_association" "public_zone1" {
  subnet_id      = aws_subnet.public_zone1.id
  route_table_id = aws_route_table.public.id
}

resource "aws_security_group" "ec2_sg" {
  name        = "ec2-security-group"
  description = "Security group for EC2"
  vpc_id      = aws_vpc.main.id

  // Ingress rules: Allow all inbound traffic (for simplicity in this example, but be more restrictive in production)
  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1" // -1 means all protocols
    cidr_blocks = ["0.0.0.0/0"]
  }

  // Egress rules: Allow all outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = {
    Name = "ec2-sg"
  }
}

// Data source for the latest Ubuntu AMI
# data "aws_ami" "ubuntu" {
#   most_recent = true
#   filter {
#     name   = "name"
#     values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
#   }
#   filter {
#     name   = "virtualization-type"
#     values = ["hvm"]
#   }
#   owners = ["099720109477"] # Canonical's AWS account ID
# }

resource "aws_key_pair" "deployer_key" {
  key_name   = "us-east-1-dev-lms-key-new" # Name of the key pair in AWS
  public_key = file(".ssh/id_rsa_terraform_new.pub") # Path to your local public SSH key
}

resource "aws_s3_bucket" "my_bucket" {
  bucket = "your-rag-pipeline-bucket"  # Must be globally unique
  acl = "private"
}

resource "aws_instance" "docker_host8" {
  ami            = "ami-020cba7c55df1f615"
  instance_type = "t3.medium" # Adjust as needed

  subnet_id     = aws_subnet.public_zone1.id
  key_name      = aws_key_pair.deployer_key.key_name
  vpc_security_group_ids = [aws_security_group.ec2_sg.id]
  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }



  # User data to install Docker and Docker Compose (bootstrkafkaing)
  # This part is crucial and ensures Docker is ready when Terraform tries to connect
  user_data = <<-EOF
             #!/bin/bash
              set -eux
              apt-get update
              apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release
              apt install dos2unix -y
              # Docker
              install -m 0755 -d /etc/apt/keyrings
              curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
              chmod a+r /etc/apt/keyrings/docker.gpg
              echo \
                "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
                $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
              apt-get update
              apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
              systemctl enable --now docker
              systemctl start docker
              systemctl enable docker
              usermod -aG docker ubuntu # Change 'ubuntu' to 'ec2-user' if using Amazon Linux AMI
              newgrp docker # This will not take effect immediately for user_data, but good for interactive sessions
              # NGINX + Certbot
              # apt-get install -y nginx certbot python3-certbot-nginx
              # systemctl enable --now nginx
              # touch /etc/nginx/sites-available/keycloak
              EOF

  tags = {
    Name = "DockerMultiplekafkasHost"
  }

  # Connection details for all provisioners
  connection {
    type        = "ssh"
    user        = "ubuntu" # Or 'ec2-user' for Amazon Linux
    private_key = file(".ssh/id_rsa_terraform_new") # Path to your local private SSH key
    host        = self.public_ip

  }
  associate_public_ip_address = true

  # # Upload files
  # provisioner "file" {
  #   source      = "install_base.sh"
  #   destination = "install_base.sh"
  # }
  # provisioner "file" {
  #   source      = "nginx_keycloak.conf.tmpl"
  #   destination = "nginx_keycloak.conf.tmpl"
  # }
  #
  # provisioner "file" {
  #   source      = "run_keycloak_first.sh"
  #   destination = "run_keycloak_first.sh"
  # }
  # provisioner "file" {
  #   source      = "echo.sh"
  #   destination = "echo.sh"
  # }
  #
  #
  provisioner "file" {
    source      = "docker-compose.yaml"
    destination = "docker-compose.yaml"
  }
  # provisioner "file" {
  #   source      = "prometheus.yaml"
  #   destination = "prometheus.yaml"
  # }
  #
  provisioner "file" {
    source = "changeIP.sh"
    destination = "changeIP.sh"
  }
  # provisioner "file" {
  #   source = "fluent-bit.conf"
  #   destination = "fluent-bit.conf"
  # }
  provisioner "file" {
    source = "docker-compose-efk.yaml"
    destination = "docker-compose-efk.yaml"
  }
  provisioner "file" {
    source = "Dockerfile-compose-monitoring.yaml"
    destination = "Dockerfile-compose-monitoring.yaml"
  }
  provisioner "file" {
    source = "docker-compose-kafka.yaml"
    destination = "docker-compose-kafka.yaml"
  }
  # Finally: run docker-compose
  provisioner "remote-exec" {

    inline = [
       "sleep 5",
      "sudo dos2unix changeIP.sh",
      "sleep 10",
      "sudo chmod +x changeIP.sh && ./changeIP.sh",
       "sleep 10",
      # "sudo docker compose -f docker-compose.yaml up -d",
      "sleep 8",
      "sudo docker run -d -p 6379:6379 redis/redis-stack-server",
      "sleep 8",
      "sudo docker compose -f docker-compose-kafka.yaml up -d",
      "sleep 15",
      "sudo docker compose -f docker-compose-efk.yaml up -d",
      "sleep 20",
      "sudo docker run --name postgres-container -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=postgres -p 5432:5432 -d postgres",
      "sleep 5"
      # "mkdir -p /home/ubuntu/chroma-data",
      # "sleep 5",
      # "sudo chown ubuntu:ubuntu /home/ubuntu/chroma-data || true",
      # "sleep 5",
      # "sudo docker pull chromadb/chroma:0.5.2",
      # "sleep 5",
      # "sudo docker rm -f chroma || true",
      # "sleep 5",
      # "sudo docker run -d --name chroma --restart unless-stopped -p 8000:8000 -v /home/ubuntu/chroma-data:/data chromadb/chroma:0.5.2",
      # "sleep 5",
  #    "sudo docker run -d --name my-rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management",
    #  "sudo docker compose -f docker-compose-kafka.yaml-compose-monitoring.yaml up -d",
   #    "sudo apt-get update -y && sudo apt-get install -y nginx certbot python3-certbot-nginx gettext docker.io",
   #    "sleep 4",
   #    "sudo systemctl enable nginx",
   #    "sleep 4",
   #    "chmod +x ~/run_keycloak_first.sh ~/echo.sh ~/install_base.sh",
   #    "sleep 5",
   #    "sudo systemctl start nginx",
   #    "sleep 4",
   #    "~/echo.sh",
   #    "sleep 5",
   #    "sudo certbot --nginx -d ${var.domain} -m =${var.letsencrypt_email} --agree-tos --non-interactive --redirect",
   # #   "sudo certbot --nginx -d kc.sujalsharma.in -m sujalsharma9109@gmail.com --agree-tos --non-interactive --redirect",
   #    "sleep 10",
   #    "sudo nginx -t",
   #    "sudo DOMAIN=${var.domain} KC_IMAGE=${var.keycloak_image} KC_ADMIN=${var.keycloak_admin_user} KC_PASS=${var.keycloak_admin_password} ~/run_keycloak_first.sh",
   #    "sleep 5",
   #    "sudo mkdir -p /etc/nginx/sites-available",
   #    "sudo cp ~/nginx_keycloak.conf.tmpl /etc/nginx/sites-available/keycloak",
   #    "sudo chmod 777 /etc/nginx/sites-available/keycloak",
   #    "sudo ln -sf /etc/nginx/sites-available/keycloak /etc/nginx/sites-enabled/keycloak",
   #    "sudo unlink /etc/nginx/sites-enabled/default",
   #    "sudo systemctl reload nginx",
   #    "sleep 5",
   #    "sudo systemctl reload nginx",
   #    "sleep 5",
   #    "sudo systemctl reload nginx"

    ]
  }
}

output "public-ip" {
  value = aws_instance.docker_host8.public_ip
}
