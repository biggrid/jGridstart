Creating a jGridstart release
=============================

When, after fixing bugs and adding new features, you'd like to bring those
changes to the general public, it is time to make a new release of jGridstart.
This would involve the following steps:


1. *Make sure that everything works.*
   When you've made changes that might involve platform-specific behaviour,
   make sure to test affected functionalities on the major platforms (Linux,
   Mac OS X and Windows). Please see `jgridstart-tests/README.md` for more
   information on that.


2. *Update version numbers.*
   Consider what modules have changed. For those modules, update the version
   number. Also update the version number where these are used as dependencies.

   If you just changed `jgridstart-main`, you can run the following commands
   (using [xmlstarlet]) to update the relevant modules to version x.y (e.g. 1.15):

        VERSION=x.y
        xml ed -P -L -N m=http://maven.apache.org/POM/4.0.0 \
            -u "/m:project/m:version" -v "$VERSION" \
            -u "/m:project/m:dependencies/m:dependency[child::m:groupId='nl.nikhef.jgridstart' \
                        and starts-with(child::m:artifactId,'jgridstart-')]/m:version" -v "$VERSION" \
            -u "/descendant::m:artifactItems/m:artifactItem[child::m:groupId='nl.nikhef.jgridstart' \
                        and starts-with(child::m:artifactId,'jgridstart-')]/m:version" -v "$VERSION" \
            jgridstart-*/pom.xml
        sed -si '/^<?xml.*$/d; s/\( xsi:schemaLocation\)/\n \1/' jgridstart-*/pom.xml

   To view the version numbers of all modules, you use the following command:
   
        xml sel -N m=http://maven.apache.org/POM/4.0.0 -t -f -o ' ' -v /m:project/m:version */pom.xml


3. *Create final version and sign resulting JAR.*
   You can either first create the jar file, and then sign it using jarsigner, or do both combined:
   1. *Separate building and signing.*
      First create the final version by running from the project's root:

          mvn clean install
     
      Now sign the resulting jar file using [jarsigner]:

          jarsigner -tsa TIMESTAMP_URL -keystore /my/codecert.jks jgridstart-jws/target/jnlp/jgridstart-wrapper-x.y.jar store_entry_name

      where TIMESTAMP_URL depends on the codesigning CA; for COMODO, it is
	  
	      http://timestamp.comodoca.com/authenticode
		  
      whereas for DigiCert it is
	  
	      http://timestamp.digicert.com
      
      Now `jgridstart-wrapper-x.y.jar` is ready for deployment.
   2. *Do all combined via mvn.*
      Make sure you have a `$HOME/.m2/settings.xml' file containing something like:

          <settings>
            <profiles>
            <profile>
                <id>codesigning</id>
                <properties>
                  <codesigning.keystore>PATH_TO_P12_STORE</codesigning.keystore>
                  <codesigning.storetype>pkcs12</codesigning.storetype>
                  <codesigning.storepass>MYPASSWORD</codesigning.storepass>
                  <codesigning.alias>CERT_ALIAS</codesigning.alias>
                </properties>
              </profile>
            </profiles>
          </settings>
          
      where `PATH_TO_P12_STORE`, `MYPASSWORD` and `CERT_ALIAS` must be replaced with the appropriate values.
      Then enable the `<sign>` blob in the `jgridstart-jws/pom.xml` file and run from the project's root:
  
          mvn -P codesigning clean install

      Now `jgridstart-wrapper-x.y.jar` is ready for deployment.

4. *Upload release.*
   Each release is uploaded to http://jgridstart.nikhef.nl/release/x.y .
   Update the URL locations in the JNLP so that it works from there:

        cd jgridstart-jws/target/jnlp
        sh deploy.sh http://jgridstart.nikhef.nl/release/x.y

   Then copy all files found in `jgridstart-jws/target/jnlp` to the location.


6. *Publish javadoc.*
   Generate javadocs by running the following command from the top-level
   directory, which puts the API documentation from all modules into
   `target/site/apidocs`:

        TITLE='jGridstart top-level x.y API'
        mvn javadoc:aggregate -Ddoctitle="$TITLE" -Dwindowtitle="$TITLE"

   Now publish it on Github using the gh-pages branch, assuming that once exists
   ([create](https://help.github.com/articles/creating-project-pages-manually)
   a new orphaned branch, if not):

        git checkout gh-pages
        rm -Rf javadoc
        mv target/site/apidocs javadoc
        git add javadoc
        git commit -m 'release documentation for jGridstart x.y' -a
        git push


7. *Update the Wiki.*
   http://jgridstart.nikhef.nl/Releases contains a list of jGridstart's
   releases. Add a new entry, make sure the links point to the correct release
   location; review source code history and add changes relevant for users and
   CAs (with bug links when present).  
   Also update http://jgridstart.nikhef.nl/Test to point to the new release.


8. *Commit and create tag* so that the source code reflects the release as well.

        git commit -m 'release x.y'
        git tag jgridstart_x.y
        git push --tags


9. *Prepare for Certificate Authority.* The DutchGrid CA uses jGridstart
   directly, and we prepare the release for them a little more. Just run the
   script `copyrelease.sh x.y` in jgridstart.nikhef.nl's `/ca` directory. Then
   you're ready to mail them!



It would be nice to eventually use the [Maven release plugin] to automate a lot of this.
For now, stick to this and you're fine.
And finally: please feel free to update and improve this file!


[xmlstarlet]: http://xmlstar.sourceforge.net/
[jarsigner]: http://docs.oracle.com/javase/7/docs/technotes/tools/solaris/jarsigner.html
[Maven release plugin]: http://maven.apache.org/plugins/maven-release-plugin
