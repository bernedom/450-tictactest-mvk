variable "region" {
  description = "AWS region. Learner Lab only allows us-east-1 and us-west-2."
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "EC2 instance type."
  type        = string
  default     = "t3.micro"
}

variable "key_name" {
  description = "Name of an existing EC2 key pair. Learner Lab provides 'vockey' (download labsuser.pem from 'AWS Details')."
  type        = string
  default     = "vockey"
}

variable "ssh_cidr" {
  description = "CIDR allowed to SSH into the instance. Narrow this to your own IP (e.g. 203.0.113.4/32)."
  type        = string
  default     = "0.0.0.0/0"
}
