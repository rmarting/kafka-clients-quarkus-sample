{{/*
Expand the name of the chart.
*/}}
{{- define "kafka-clients-quarkus-sample-chart.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "kafka-clients-quarkus-sample-chart.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "kafka-clients-quarkus-sample-chart.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "kafka-clients-quarkus-sample-chart.labels" -}}
helm.sh/chart: {{ include "kafka-clients-quarkus-sample-chart.chart" . }}
{{ include "kafka-clients-quarkus-sample-chart.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
group: io.jromanmartin.kafka
app: kafka-clients-quarkus-sample
{{- end }}

{{/*
Selector labels
*/}}
{{- define "kafka-clients-quarkus-sample-chart.selectorLabels" -}}
app.kubernetes.io/part-of: {{ include "kafka-clients-quarkus-sample-chart.name" . }}
app.kubernetes.io/name: {{ include "kafka-clients-quarkus-sample-chart.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.openshift.io/runtime: java
app.openshift.io/runtime-version: openjdk-11-el7
{{- end }}
