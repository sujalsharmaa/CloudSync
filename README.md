# drive.sujalsharma.in

order for k8s deployment

alias k=kubectl

upload secrets

k apply -f .

git clone https://github.com/sujalsharmaa/CloudSync.git

kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.1/deploy/static/provider/cloud/deploy.yaml

kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.5/cert-manager.yaml

k apply -f persistent-volume.yaml
k apply -f persistent-volume-claim.yaml

cd postgres

k apply -f .

cd Redis-Manifests

k apply -f .

cd Kafka

k apply -f .

cd Elasticsearch

k apply -f .

cd nginx 

k apply -f .

cd configmaps.yaml

k apply -f .

then go to kubernetes manifests

k apply -f .

k get ing

update all domains in vercel

auth.sujalsharma.in
upload.sujalsharma.in
file.sujalsharma.in
payment.sujalsharma.in
search.sujalsharma.in

# "default" for Azure "standard" for GCP and "standard" for AWS