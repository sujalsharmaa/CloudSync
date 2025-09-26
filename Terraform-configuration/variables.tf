variable "domain" {
  type        = string
  default     = "kc.sujalsharma.in"
  description = "FQDN for Keycloak"
}

variable "letsencrypt_email" {
  type        = string
  description = "Email for Let’s Encrypt registration"
  default = "sujalsharma9109@gmail.com"
}

variable "keycloak_admin_user" {
  type        = string
  default     = "admin"
}

variable "keycloak_admin_password" {
  type        = string
  default = "admin"
}

variable "keycloak_image" {
  type        = string
  default     = "quay.io/keycloak/keycloak:26.0.5"
  description = "Keycloak image tag"
}