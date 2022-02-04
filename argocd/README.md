# ArgoCD

**NOTE**: This section requires to have deployed the ArgoCD operator. You could use the
[Helm Chart opp-chart](../charts/opp-chart) to deploy the required Kubernetes Operators.

1. Setup the ArgoCD operator to manage the `tools-ci-cd` namespace and disabled the 
cluster wide instance:

* Getting current namespaces managed by the operator

```shell
oc get csv/openshift-gitops-operator.v1.4.1 -n openshift-operators \
    -o jsonpath='{.spec.install.spec.deployments[?(@.name=="gitops-operator-controller-manager")].spec.template.spec.containers[?(@.name=="manager")].env[?(@.name=="ARGOCD_CLUSTER_CONFIG_NAMESPACES")].value}'

export ARGOCD_NS="$(oc get csv/openshift-gitops-operator.v1.4.1 -n openshift-operators \
    -o jsonpath='{.spec.install.spec.deployments[?(@.name=="gitops-operator-controller-manager")].spec.template.spec.containers[?(@.name=="manager")].env[?(@.name=="ARGOCD_CLUSTER_CONFIG_NAMESPACES")].value}'),tools-ci-cd"
```

* Patching operator to manage the other namespaces, like `tools-ci-cd`:

```shell
oc patch csv openshift-gitops-operator.v1.4.1 -n openshift-operators \
    --type='json' \
    -p '[{"op": "replace", "path": "/spec/install/spec/deployments/0/spec/template/spec/containers/0/env/22", "value": {"name": "ARGOCD_CLUSTER_CONFIG_NAMESPACES", "value":"'${ARGOCD_NS}'"}}]'
```

If you want to uninstall the default ArgoCD instance, add the following env variable in the CSV:

```shell
oc patch csv openshift-gitops-operator.v1.4.1 -n openshift-operators --type=json \
    -p '[{"op": "add", "path": "/spec/install/spec/deployments/0/spec/template/spec/containers/0/env/-", "value": {"name":"DISABLE_DEFAULT_ARGOCD_INSTANCE","value":"true"}}]'
```

The definition of the operator should be similar to:

```yaml
apiVersion: operators.coreos.com/v1alpha1
kind: ClusterServiceVersion
metadata:
spec:
  install:
    spec:
      deployments:
        - name: gitops-operator-controller-manager
          spec:
            template:
              spec:
                containers:
                  - name: manager
                    env:
                      - name: ARGOCD_CLUSTER_CONFIG_NAMESPACES
                        value: 'openshift-gitops,tools-ci-cd'
                      - name: DISABLE_DEFAULT_ARGOCD_INSTANCE
                        value: 'true'
```

2. Deploy a new instance of ArgoCD in the `tools-ci-cd` namespace to deploy applications:

```shell
oc new-project tools-ci-cd
oc apply -f argocd.yaml -n tools-ci-cd
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

Register the production cluster

```shell
oc config rename-context $(oc config current-context) ocp-pro
argocd cluster add ocp-pro
```

3. Create the namespaces and grant ArgoCD to admin them.

For demo environment:

```shell
oc new-project amq-streams-demo
oc label namespace amq-streams-demo argocd.argoproj.io/managed-by=argocd
oc adm policy add-role-to-user admin system:serviceaccount:tools-ci-cd:argocd-argocd-application-controller -n amq-streams-demo
```

For development environment:

```shell
oc new-project amq-streams-dev
oc label namespace amq-streams-dev argocd.argoproj.io/managed-by=argocd
oc adm policy add-role-to-user admin system:serviceaccount:tools-ci-cd:argocd-argocd-application-controller -n amq-streams-dev
```

For testing environment:

```shell
oc new-project amq-streams-tst
oc label namespace amq-streams-tst argocd.argoproj.io/managed-by=argocd
oc adm policy add-role-to-user admin system:serviceaccount:tools-ci-cd:argocd-argocd-application-controller -n amq-streams-tst
oc policy add-role-to-user system:image-puller system:serviceaccount:amq-streams-tst:default -n amq-streams-dev
```

For production environment:

```shell
oc new-project amq-streams-pro
oc label namespace amq-streams-pro argocd.argoproj.io/managed-by=argocd
oc adm policy add-role-to-user admin system:serviceaccount:tools-ci-cd:argocd-argocd-application-controller -n amq-streams-pro
oc policy add-role-to-user system:image-puller system:serviceaccount:amq-streams-pro:default -n amq-streams-dev
```

4. Create a sample application to test and verify ArgoCD is working successfully:

```shell
oc apply -f applications/sample-app-application.yaml -n tools-ci-cd
```

5. Create the Application project:

```shell
oc apply -f projects/ -n tools-ci-cd
```

6. Create the new application to deploy the services needed for the application (Kafka, Topics, Users and Registry):

Applying the `application` CR object describing the application:

```shell
oc apply -f applications/quarkus-eda/<PLATFORM>/<ENV>/application-eda.yaml
oc apply -f applications/quarkus-eda/<PLATFORM>/<ENV>/application-app.yaml
```

Or use the `argocd` CLI to create the definition of the application:

```shell
argocd app create kafka-clients-quarkus-sample-eda-<ENV> \
  --project kafka-clients-quarkus \
  --dest-namespace amq-streams-<ENV> \
  --dest-server https://kubernetes.default.svc \
  --repo https://github.com/rmarting/kafka-clients-quarkus-sample.git \
  --path charts/eda-chart --values values-<PLATFORM>.yaml \
  --sync-policy automated

argocd app create kafka-clients-quarkus-sample-app-<ENV> \
  --dest-namespace amq-streams-<ENV> \
  --dest-server https://kubernetes.default.svc \
  --repo https://github.com/rmarting/kafka-clients-quarkus-sample.git \
  --path "charts/app-chart" \
  --sync-policy automated --self-heal --auto-prune
```

Choose the right value for `PLATFORM` variable:

* `crc` defined to be use for your local CodeReady Workspaces.
* `k8s` defined to be used for your local Minishift instance.
* `ocp` defined to be used for an OpenShift cluster.

Choose the right value for `ENV` variable:

* `dev` defined a development environment.
* `tst` defined a testing environment.

Verify the applications are synchronized in the final environments:

```shell
❯ oc get applications
NAME                                   SYNC STATUS   HEALTH STATUS
kafka-clients-quarkus-sample-app-dev   OutOfSync     Healthy
kafka-clients-quarkus-sample-app-tst   OutOfSync     Degraded
kafka-clients-quarkus-sample-eda-dev   Synced        Healthy
kafka-clients-quarkus-sample-eda-tst   Synced        Healthy
sample-app                             Synced        Healthy
```

Now you could test the apps from each environment following the instructions from
the [README](../README.md) file.

8. You could add a webhook to enable push actions to ArgoCD. It requires to create a
secret with a value to be used from the Git WebHook.

```shell
oc apply -f webhooks/ -n amq-streams-dev
```

Extract the value from the secret:

```shell
oc get secret kafka-clients-quarkus-sample-generic-webhook-secret \
  -n amq-streams-dev \
  -o jsonpath='{.data.WebHookSecretKey}' | base64 -d
```

Invoke the webhook:

```shell
curl -X POST \
  -k $(oc whoami --show-server)/apis/build.openshift.io/v1/namespaces/amq-streams-dev/buildconfigs/kafka-clients-quarkus-sample/webhooks/$(oc get secret kafka-clients-quarkus-sample-generic-webhook-secret -n amq-streams-dev -o jsonpath='{.data.WebHookSecretKey}' | base64 -d)/generic
```
