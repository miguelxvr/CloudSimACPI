#/bin/sh

mvn -U  -Dmaven.test.skip=true clean install compile package
#cd target
##jar -uf cloudsim-new.jar -C ../lib/jdom-1.1.2/ org/
jar -uf target/cloudsim-3.1-SNAPSHOT.jar -C lib/jdom-1.1.2/ org/
jar -uf target/cloudsim-3.1-SNAPSHOT.jar -C lib/ algs-1.0/
jar -uf target/cloudsim-3.1-SNAPSHOT.jar -C lib/ stdlib-1.0/

