mvn -B -q exec:java -Dexec.mainClass="nl.nikhef.jgridstart.gui.Main" -Dexec.args="$@" -Djgridstart.requestwizard.provider=nl.nikhef.jgridstart.ca.confusa.RequestWizard -Djgridstart.ca.provider=nl.nikhef.jgridstart.ca.confusa.ConfusaCA