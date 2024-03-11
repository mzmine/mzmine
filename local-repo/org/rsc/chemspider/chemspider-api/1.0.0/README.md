Generated using these commands:

wget http://central.maven.org/maven2/io/swagger/swagger-codegen-cli/2.3.1/swagger-codegen-cli-2.3.1.jar -O swagger-codegen-cli.jar
java -jar swagger-codegen-cli.jar generate -l java -i https://api.rsc.org/compounds/v1 --api-package org.rsc.chemspider.api --model-package org.rsc.chemspider.api --group-id org.rsc.chemspider --artifact-id chemspider-api
mvn package

