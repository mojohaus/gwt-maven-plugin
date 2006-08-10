GWT_HOME=/usr/local/Google/gwt-linux-1.0.21

mvn install:install-file -Dfile=$GWT_HOME/gwt-user.jar -DgroupId=com.google.gwt -DartifactId=gwt-user -Dversion=1.0.21 -Dpackaging=jar -DgeneratePom=true -DcreateChecksum=true
mvn install:install-file -Dfile=$GWT_HOME/gwt-dev-linux.jar -DgroupId=com.google.gwt -DartifactId=gwt-dev-linux -Dversion=1.0.21 -Dpackaging=jar -DgeneratePom=true -DcreateChecksum=true
