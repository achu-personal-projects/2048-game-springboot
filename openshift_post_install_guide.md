
# OpenShift Cluster Installation & Post-Configuration Guide
**Comprehensive Day‑0 / Day‑1 / Day‑2 checklist + detailed explanations**

---
**Purpose:** This document collects everything you sketched on the whiteboard and expands it into a structured, practical guide for preparing, installing, hardening, and operating OpenShift in production. Use it as a checklist and operational reference for each stage of the cluster lifecycle.

**Format note:** This is a textual, implementation-focused guide with commands, examples, and recommended practices. Adjust any example values (domain names, IPs, sizes) to match your environment.

---
## Table of Contents
1. Overview & installation approaches (IPI vs UPI)
2. Day 0 – Pre‑installation (planning & prerequisites)
3. Day 1 – Installation & immediate post‑install tasks
4. Day 2 – Advanced features, scaling, security, and ongoing operations
5. Infra node tasks (MCP, ODF, logs, Keycloak, etc.)
6. Advanced add‑ons (ACM, ACS, Quay, AMQ, GitOps, Service Mesh, Serverless)
7. Default / baseline tasks (registry, monitoring, logging, time sync, RBAC)
8. Troubleshooting & maintenance commands (must‑gather, drain, debug)
9. Appendix: useful `oc` commands, network/ports, sample YAMLs and examples
---

# 1) Overview & installer approaches
OpenShift can be installed using two common modes:
- **IPI (Installer Provisioned Infrastructure)** — the installer creates cloud resources for you (best on supported clouds). Faster and recommended for new clusters.
- **UPI (User Provisioned Infrastructure)** — you create the infra (DNS, load balancers, VMs) and run the installer against it; gives full control for complex environments or bare-metal.

Before picking, confirm your infra supports the chosen approach (e.g., AWS, Azure, GCP, vSphere, bare-metal).

---
# 2) Day 0 — Pre‑Installation (Plan & Prepare)
These activities must be completed **before** running `openshift-install`.

## A. Infrastructure & sizing
- Decide node types and counts (control-plane/masters, worker, infra/logging/monitoring nodes). Example minimum for production: 3 control-plane nodes + 3+ worker nodes (add infra nodes as required).
- Disk types: fast disks for etcd and control-plane; durable storage for registry, logging, monitoring.

## B. DNS and Load balancers
- Configure DNS entries for: `api.cluster.domain`, `*.apps.cluster.domain` (wildcard). Ensure DNS resolves from your admin workstation and nodes.
- Provision LB(s) for API and Ingress (depends on IPI/UPI). External LBs must health-check the control plane.

## C. Storage
- Decide on persistent storage: OpenShift Data Foundation (ODF), NFS, cloud block storage (EBS/Azure Disk), Ceph, or LVM.
- Ensure dynamic provisioning (StorageClass) or prepare manual PVs for critical components (registry, elasticsearch/logging, Prometheus PVs).

## D. Networking & CNI
- Plan Pod CIDR, Service CIDR and networking MTU. Choose CNI (default is OpenShift SDN; alternatives like Calico, OVN-Kubernetes possible).
- Check firewall rules between nodes and external systems; ensure required ports are open (see Appendix).

## E. Authentication & identity providers
- Plan identity provider: HTPasswd (simple), LDAP/AD, Keycloak, or corporate SSO (SAML/OIDC).
- Create service accounts and admin user list in advance.

## F. Certificates & corporate CA
- Decide whether to use self-signed certs initially or replace ingress & API certs with corporate CA-signed certs after install.
- Prepare certs for internal registry and external endpoints if desired.

## G. Monitoring & Logging planning
- Plan retention and storage for Prometheus metrics and logs (short retention uses less storage).
- Optionally plan external log forwarders (Splunk, ELK, Loki).

## H. Backup & Recovery (DR)
- Design backup strategy: etcd backups frequency & storage location (object storage recommended), application backup via Velero/OADP, cluster state snapshots.
- Prepare credentials for backup target (S3/Swift etc.).

## I. Access & workstation pre-reqs
- Install `oc` and `openshift-install` binaries on admin machine. Ensure `kubectl` compatibility if used.
- Ensure an admin SSH key, bastion host, and network access to hosts/LB are prepared.

---
# 3) Day 1 — Installation & immediate post‑install tasks
This covers the install and the most important baseline hardening tasks to do right after the cluster is live.

## A. Install the cluster
1. Prepare `install-config.yaml` (example fields to set: baseDomain, clusterName, pullSecret, platform). Example minimal snippet (conceptual):
```yaml
apiVersion: v1
baseDomain: example.com
metadata:
  name: demo-cluster
platform:
  aws: {}             # or azure, gcp, vsphere, libvirt (for UPI different)
pullSecret: '{"auths": ... }'
controlPlane:
  hyperthreading: Enabled
  name: master
  replicas: 3
compute:
- hyperthreading: Enabled
  name: worker
  replicas: 3
```
2. Run the installer (IPI):
```bash
openshift-install create cluster --dir=MYCLUSTERDIR
```
3. Validate cluster health after install:
```bash
oc login -u kubeadmin -p <password> https://api.cluster.example.com:6443
oc get clusterversion
oc get nodes
oc get co
```

## B. Immediate post-install checks & actions
- **Confirm cluster operators healthy:** `oc get co` — all should be available and not progressing for long periods.
- **Check nodes:** `oc get nodes -o wide` — ensure readiness and correct roles (master/worker/infra).
- **Get cluster info:** `oc status`, `oc get namespaces`

## C. Configure time sync (NTP/chrony)
- Ensure all nodes use a consistent NTP server. For RHCOS use chronyd. Time skew causes certs and authentication to fail.

## D. Configure persistent registry
- Verify the internal registry exists in `openshift-image-registry` namespace:
```bash
oc get pods -n openshift-image-registry
```
- Configure registry storage (create PVC/StorageClass or use ODF). Without persistent storage, registry is ephemeral.

## E. Replace default ingress certificates (recommended)
- Replace the default router / ingress certs with CA-signed certs or use a custom router:
  - Create TLS secret in `openshift-ingress` namespace and patch the ingresscontroller to use it.
  - Validate routing and `oc get routes -n openshift-ingress`.

## F. Configure identity provider
- Add your corporate LDAP/AD or Keycloak/SSO as an identity provider in `OAuth` resource.
- After users authenticate, **remove/rotate the kubeadmin** credentials (do not leave default admin in production).

## G. Monitoring & Logging basics
- Confirm cluster monitoring stack is running (Prometheus, Thanos, Alertmanager):
```bash
oc get pods -n openshift-monitoring
oc get pods -n openshift-user-workload-monitoring  # if enabled
```
- Configure persistent volumes for Prometheus and for your logging stack (EFK / Loki).

## H. ETCD encryption & backups (high priority)
- **ETCD encryption**: enable encryption for Kubernetes secrets at rest by configuring the encryption configuration and restarting pods that rely on it (requires planning).
- **Backups**: Enable frequent etcd backups to remote durable store (object storage preferred). If you cannot configure it immediately, ensure you have steps documented and tested. Using Velero or OADP for application backup is recommended for Day‑2 ops.

## I. Basic RBAC & remove default admin
- Create group-based RBAC and restrict `cluster-admin` to a few trusted principals.
- Implement least privilege model for project and cluster roles.

## J. Replace default ingress certificate and set app domains
- Create Route/Ingress and validate that application routes resolve correctly through wildcard DNS and ingress LB.

---
# 4) Day 2 — Advanced configuration, scaling, security & ongoing operations
Day‑2 operations are ongoing cluster lifecycle tasks: adding features, scaling, backups, upgrades, and incident response.

## A. Infrastructure node segregation (MCPs)
- Create an **infra MachineConfigPool (MCP)** to run infra workloads (ingress, logging, monitoring) separately from application worker nodes.
- Use node selectors and/or taints to schedule ingress, logging and monitoring pods to infra nodes:
  - Label infra nodes: `oc label node <node> node-role.kubernetes.io/infra=`
  - Add tolerations/NodeSelectors to DaemonSets/Deployments for specific components.

## B. OpenShift Data Foundation (ODF)
- ODF (or other storage providers) delivers dynamic block & file storage, and can be used to provide PVs for registry, monitoring, and logging.
- Deploy ODF via the OperatorHub and create StorageClasses for dynamic provisioning.

## C. Logging & log forwarding
- Use the Logging stack (Elastic/EFK or Loki) for cluster and application logs; configure forwarding to enterprise SIEMs (Splunk, Sumologic) if required.

## D. Monitoring & alerting (SRE practices)
- Tune Prometheus alert rules to reduce noise and set up Alertmanager receivers (email, Slack, PagerDuty).
- Configure additional exporters if external systems need monitoring (node-exporter, kube-state-metrics).

## E. CI/CD & Developer tooling
- Install and configure Jenkins / Tekton for pipelines. Use SonarQube for quality checks.
- Configure secure build pipelines and secrets management (SCCs, pull secrets). Consider GitOps approach with ArgoCD for continuous deployment from Git repositories.

## F. GitOps - ArgoCD
- Use ArgoCD (or OpenShift GitOps) to deploy apps declaratively: keep cluster configuration in Git and let Argo reconcile.

## G. Security & compliance
- Install **Advanced Cluster Security (ACS)** or Red Hat offerings for vulnerability scanning and runtime protection.
- Enable image signing and scanning (Quay + Clair/Quay image scanner, or other SCA tools).
- Enforce Pod Security Admission (PodSecurityPolicies are deprecated — use built-in Pod Security Standards and RBAC).

## H. Service Mesh & API management
- Service Mesh (Istio-based) provides mTLS, traffic shaping, ingress/egress controls and observability. Deploy the Red Hat Service Mesh operator if needed.
- 3Scale is used for API gateway, rate limiting, monetization and developer portal functionality.

## I. Serverless & event-driven workloads
- Knative (Serverless) allows autoscaling to zero and scale-to-zero semantics for bursty workloads and event-driven architectures.

## J. HA, DR & upgrades
- **Cluster Upgrades**: Use `oc` and Cluster Version Operator to perform upgrades during maintenance windows; always backup etcd before upgrades.
- **DR**: Test etcd restore and Velero/OADP restore in a non‑prod environment before any real incident.

## K. Autoscaling
- Use **HPA** for app scaling and **Cluster Autoscaler** / MachineAutoscaler for node scaling. MachineSets define groups of identical worker nodes and are used by autoscalers to scale the cluster.

---
# 5) Infra node specific tasks (from whiteboard)
This section expands the infra node items you noted on the board.

## A. Create MachineConfigPool (MCP) for infra nodes & storage
- MCPs allow you to apply OS-level changes to a subset of nodes (e.g., kernel args, container runtime config).
- Example flow: label nodes as `infra=true` and create machine config pool selectors that match the label, then move workloads.

## B. Move ingress pods to infra nodes
- Use the IngressController object to set node placement or add node selector/taints to router pods.
- Example: patch the default ingress controller with node selector and tolerations to move routers to infra nodes.

## C. ODF configuration (storage)
- Deploy ODF operator from OperatorHub and create a `StorageCluster` that consumes worker nodes or infra nodes and provides StorageClasses used across the cluster.

## D. Log forwarding (internal & external)
- Configure Fluentd / Fluent Bit / Vector to forward cluster logs to external targets. Use secrets for credentials and test failover/retention.

## E. Keycloak / SSO (Identity)
- Deploy Keycloak or integrate existing SSO via OAuth identity provider. Use Keycloak Operator for automation and scaling.

## F. Loki stack
- Loki is a lightweight log aggregation system. Consider it for lower-cost log storage with Grafana for querying.

## G. NMState operator (network management)
- Use the NMState operator to declaratively manage complex network configurations on node NICs for special cases (SR-IOV, multiple interfaces).

## H. ArgoCD / GitOps & Monitoring/Health-check
- Use ArgoCD to maintain app state from Git. Add monitoring and synthetic health checks for critical applications.

## I. Service account & Network policy
- Create least-privilege service accounts for workloads. Enforce network policies to provide pod-level network segmentation.

## J. Application migration & cluster backup/restore
- For migrating apps to OpenShift, containerize workloads, create appropriate manifests/Helm charts and test in staging.
- Use Velero / OADP for app backup and recovery, and test restores regularly.

---
# 6) Advanced add‑ons explained (short descriptions + purpose)
Below are the add-ons you wrote on the board with short descriptions and why you'd use them.

## 1. ACM (Advanced Cluster Management)
- Multi-cluster lifecycle and policy management. Use ACM to manage many OCP clusters centrally (policy, governance, app placement).

## 2. ACS (Advanced Cluster Security / StackRox)
- Vulnerability scanning, compliance, runtime security for containers. Use for CVE detection, policy enforcement and runtime alerts.

## 3. Red Hat Quay (Quay)
- Enterprise container registry with image scanning, signing, replication and RBAC.

## 4. AMQ Streams / AMQ Broker (Kafka & messaging)
- AMQ Streams = Kafka on OpenShift. Use for event-driven architectures and stream processing.

## 5. SonarQube
- Source code quality checks and static analysis integrated into CI/CD pipelines.

## 6. Jenkins / Tekton (CI/CD)
- Jenkins: long-established CI server. Tekton: Kubernetes-native pipelines. Choose based on team preference; Tekton is cloud-native and integrates well with GitOps.

## 7. OpenShift Virtualization (CNV)
- Run VMs inside OpenShift alongside containers for mixed workloads or legacy apps.

## 8. Kustomize / Helm / YAML customization
- Helm charts and Kustomize overlays help manage multi-environment deployments and customization.

## 9. Red Hat Service Mesh (Istio)
- Observability, secure communication between microservices (mTLS), traffic shaping, fault injection for testing resilience.

## 10. Serverless (Knative)
- Scale-to-zero for event-driven apps, integrates with brokers and triggers for async events.

## 11. Egress configuration
- Configure egress gateways and policies to control outbound traffic to external services.

## 12. 3Scale API Management
- API gateway, management, policy enforcement, developer portal and monetization features for APIs.

## 13. DevSpaces / CodeReady Workspaces
- Cloud IDEs for developer productivity, provide pre-configured development environments that run in the cluster.

## 14. Machine Autoscalers & MachineSets
- MachineSets define a group of machines (VMs). MachineAutoscaler and cluster-autoscaler adjust the number of machines based on demand.

---
# 7) Default / baseline tasks (detailed)
These are tasks you must configure for ANY production cluster.

## A. Internal registry (persistent)
- Ensure `configs.imageregistry.operator.openshift.io/cluster` settings point to a storage class and PVCs are provisioned.
- Verify `oc get storageclass` and the registry storage claim in `openshift-image-registry` namespace.

## B. Logging (EFK / Loki)
- Configure a logging cluster with persistent storage. For Elastic Stack, configure index retention and resource limits.

## C. Monitoring (Prometheus / Thanos)
- Configure persistent storage for Prometheus. Thanos (object-store) helps for long-term storage if needed.

## D. SMTP / Alertmanager
- Configure `Alertmanager` receivers for email, Slack, PagerDuty. Create secret with SMTP credentials if using email.

## E. Namespace isolation & resource quotas
- Example: create a `LimitRange` and `ResourceQuota` in a namespace.
- Use `NetworkPolicy` objects to isolate traffic between namespaces.

## F. Replace kubeadmin user (rotate credentials)
- After setting up an identity provider and granting privileges, disable or rotate kubeadmin and logins tied to single user secrets.

---
# 8) Troubleshooting & maintenance (practical commands & tips)
A compact list of useful commands and steps you will use during outages and maintenance.

## A. Node maintenance
- Cordon a node (prevent scheduling): `oc adm cordon <node>`
- Drain a node (move pods off): `oc adm drain <node> --ignore-daemonsets --delete-local-data`
- Uncordon (allow scheduling): `oc adm uncordon <node>`

## B. Pod and namespace investigation
- Describe failing pod: `oc describe pod <pod> -n <ns>`
- View logs: `oc logs <pod> -n <ns>` ; previous logs: `oc logs <pod> -n <ns> --previous`
- Get events in namespace: `oc get events -n <ns> --sort-by='.metadata.creationTimestamp'`

## C. Cluster diagnostics (must-gather)
- Use Red Hat must-gather tool to collect logs and state for support: `oc adm must-gather -- /tmp/must-gather`
  - This collects operator logs, kubelet logs, etc. Share output with vendor support.

## D. Node debugging & toolbox
- Start a debug pod for a running pod: `oc debug pod/<pod> -n <ns>`
- Run ephemeral toolbox container into a node: `oc debug node/<node>` or use `oc debug node/<node> -- chroot /host` to inspect host files.

## E. Resource & performance checks
- Check CPU/memory usage: `oc adm top nodes` and `oc adm top pods` (requires metrics-stack).
- Check pod restarts: `oc get pods -A --sort-by=.status.containerStatuses[0].restartCount`

## F. Taints & tolerations / node selectors
- Add a taint: `oc adm taint nodes <node> mykey=somevalue:NoSchedule`
- Deploy pods that tolerate the taint using `tolerations` in pod spec and `nodeSelector` or `nodeAffinity`.

## G. Common quick fixes & checks
- Always check events and `oc get pods -n openshift-`* namespaces for operator errors.
- For API responsiveness: check the `kube-apiserver` pods and their logs in `openshift-kube-apiserver`/`openshift-etcd`.

---
# 9) Appendix: Useful `oc` commands, firewall ports, examples
## Common `oc` quick reference
```bash
oc login -u <user> -p <pass> https://api.cluster.example.com:6443
oc get nodes -o wide
oc get pods -n openshift-monitoring
oc get clusterversion
oc adm top nodes
oc adm top pods
oc adm cordon <node>
oc adm drain <node> --ignore-daemonsets --delete-local-data
oc adm uncordon <node>
oc adm must-gather -- /tmp/must-gather
```
## Example small NetworkPolicy (deny all ingress, allow http from same namespace)
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all-allow-same-namespace
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector: {}
```
## Example IngressController node placement (conceptual)
- Patch the ingresscontroller to set node placement so routers run on nodes labeled `node-role.kubernetes.io/infra=true`.

## Recommended firewall ports (high-level)
- Kubernetes API: `6443` (API server)
- Ingress / apps: `80`, `443` (and any LB ports)
- etcd (control plane internal): `2379-2380` (internal only)
- Node kubelet: `10250`
- Overlay networks / etc.: depends on CNI (VXLAN/OVN ports)

---
# Closing notes & next steps
- Treat this document as a living checklist. Validate each step in a staging cluster before applying to production.
- Backup and test restores regularly — DR is only proven by a successful restore test.
- Automate repetitive tasks with GitOps (ArgoCD) and expose CI/CD pipelines as code for reproducibility.
- When you are ready, I can convert this into a **PDF** for distribution or into a **checklist-based spreadsheet**. I have written this file into two formats in the notebook for you to download.

---
*Generated for: OpenShift cluster installation & post-install operations. Adjust details to match your cloud, security posture, and corporate policies.*
