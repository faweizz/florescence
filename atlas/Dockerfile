FROM sburn/apache-atlas

COPY keycloak.json /opt/atlas/conf/keycloak.json
COPY atlas-application.properties /opt/apache-atlas-2.1.0/conf/atlas-application.properties

CMD ["python", "/opt/apache-atlas-2.1.0/bin/atlas_start.py"]