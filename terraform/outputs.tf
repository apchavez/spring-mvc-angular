output "cluster_name" {
  description = "EKS cluster name"
  value       = module.eks.cluster_name
}

output "cluster_endpoint" {
  description = "EKS control plane endpoint"
  value       = module.eks.cluster_endpoint
}

output "region" {
  description = "AWS region the cluster was created in"
  value       = var.aws_region
}

output "configure_kubectl" {
  description = "Command to point kubectl/helm at this cluster"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

output "ingress_nginx_load_balancer_hint" {
  description = "How to find the ingress-nginx controller's external address once installed"
  value       = "kubectl get svc -n ingress-nginx ingress-nginx-controller -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'"
}
