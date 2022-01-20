# Quarkus Clients Kafka Sample Helm Charts

Helm Charts to manage application deployments in Kubernetes or OpenShift.

This Helm Chart was tested with 

```shell
❯ helm version
version.BuildInfo{Version:"v3.7.2", GitCommit:"663a896f4a815053445eec4153677ddc24a0a361", GitTreeState:"clean", GoVersion:"go1.16.10"}
```

This repo was tested with the following latest versions of Red Hat CodeReady Containers and Minikube:

```shell
❯ crc version
CodeReady Containers version: 1.36.0+c0f4e0d3
OpenShift version: 4.9.8 (embedded in executable)
❯ minikube version
minikube version: v1.24.0
commit: 76b94fb3c4e8ac5062daf70d60cf03ddcc0a741b
```

and OpenShift:

```shell
❯ oc version
Client Version: 4.5.3
Server Version: 4.9.0
Kubernetes Version: v1.22.0-rc.0+894a78b
```

Main References:

* [Helm - The package manager for Kubernetes](https://helm.sh/)
* [Helm - Getting Started](https://helm.sh/docs/chart_template_guide/getting_started/)
* [Helm Version Support Policy](https://helm.sh/docs/topics/version_skew/)
* [Helm Commands](https://helm.sh/docs/helm/)

## Deploying Application

Install or Upgrade the application with this Helm Chart for your current environment, setting the
right value for `ENV` variable:

* `crc` defined to be use for your local CodeReady Workspaces.
* `k8s` defined to be used for your local Minishift instance.
* `ocp` defined to be used for an OpenShift cluster.

1. Deploy the Kubernetes Operators to manage the rest of services and applications needed. 

```shell
❯ helm upgrade --install kafka-clients-quarkus-sample-ops ./opp-chart \
    -f ./opp-chart/values-ocp.yaml \
    --history-max 4 --namespace amq-streams-demo --create-namespace
```

2. Deploy the Event-Driven services needed for the application

```shell
❯ helm upgrade --install kafka-clients-quarkus-sample-eda ./eda-chart \
    -f ./eda-chart/values-ocp.yaml \
    --history-max 4 --namespace amq-streams-demo --create-namespace
```

3. Deploy the application

```shell
❯ helm upgrade --install kafka-clients-quarkus-sample-app ./app-chart \
    --history-max 4 --namespace amq-streams-demo --create-namespace
```

## Removing application

To remove the helm chart

```shell
helm uninstall kafka-clients-quarkus-sample-app
helm uninstall kafka-clients-quarkus-sample-eda
helm uninstall kafka-clients-quarkus-sample-ops
```
