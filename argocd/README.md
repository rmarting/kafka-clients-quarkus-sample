# ArgoCD

**NOTE**: This section requires to have deployed the ArgoCD operator. You could use the
[Helm Chart opp-chart](../charts/opp-chart) to deploy the required Kubernetes Operators.


1. Deploy a new instance of ArgoCD to use in our namespace:

```shell
oc apply -f argocd.yaml
```

ArgoCD should need grants to admin the namespace, in that case, execute the following command:

```shell
oc adm policy add-role-to-user admin system:serviceaccount:amq-streams-demo:argocd-argocd-application-controller -n amq-streams-demo
```

To access to the new ArgoCD instance:

```shell
echo https://$(oc get route argocd-server -o jsonpath='{.spec.host}')
```

Also you could login with the default `admin` user created (the default password is stored in a secret):

```shell
argocd login $(oc get route argocd-server -o jsonpath='{.spec.host}') \
  --username admin \
  --password $(oc get secret argocd-cluster -o jsonpath='{.data.admin\.password}' | base64 -d) \
  --insecure
```

2. Create a new application to deploy the services needed for the application (Kafka, Topics, Users and Registry):

Create the `application` CR object describing the application:

```shell
oc apply -f application-eda/application-eda-<ENV>.yaml
```

Or use the `argocd` CLI to create the definition of the application:

```shell
argocd app create kafka-clients-quarkus-sample-eda \
  --dest-namespace amq-streams-demo \
  --dest-server https://kubernetes.default.svc \
  --repo https://github.com/rmarting/kafka-clients-quarkus-sample.git \
  --path "charts/eda-chart" --values "values-<ENV>.yaml" \
  --sync-policy automated --self-heal --auto-prune
```

Choose the right value for `ENV` variable:

* `crc` defined to be use for your local CodeReady Workspaces.
* `k8s` defined to be used for your local Minishift instance.
* `ocp` defined to be used for an OpenShift cluster.

3.- Create our application:

Create the `application` CR object describing the application:

```shell
oc apply -f application-app/application-app.yaml
```

Or use the `argocd` CLI to create the definition of the application:

```shell
argocd app create kafka-clients-quarkus-sample-app \
  --dest-namespace amq-streams-demo \
  --dest-server https://kubernetes.default.svc \
  --repo https://github.com/rmarting/kafka-clients-quarkus-sample.git \
  --path "charts/app-chart" \
  --sync-policy automated --self-heal --auto-prune
```
