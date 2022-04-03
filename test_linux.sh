#!/bin/bash

#Setting up variables to use
if [ "$#" -ne 1 ]; then
    echo "Incorrect number of arguments, specify STUDENT_DIR TEST_NAME"
else
    STUDENT_DIR=$1
    TEST_NAME="IOCoverageTest"
    S_SANDBOX="$(basename $1)_sandbox"
    B_NAME=$(basename $STUDENT_DIR)

    #Clean .class files
    cd $STUDENT_DIR
    rm *.class 2> /dev/null
    cd -


    #Construct the sandbox

    rm -rf $S_SANDBOX 2> /dev/null
    mkdir $S_SANDBOX
    cd $S_SANDBOX
    cp -r ../$STUDENT_DIR ./$B_NAME
    cp ../IOCoverageTest.java ./$B_NAME/IOCoverageTest.java
    cp ../ExitException.java ./$B_NAME/ExitException.java
    cp ../NoExitSecurityManager.java ./$B_NAME/NoExitSecurityManager.java
    mkdir ./bin
    mkdir ./tests


    #Compute the test coverage
    javac -cp $B_NAME:../junit/junit-4.12.jar:../junit/hamcrest-core-1.3.jar $B_NAME/$TEST_NAME.java -d ./bin
    cp -r ../$STUDENT_DIR/tests ./tests
    cp $B_NAME/*.csv ./ ; cp $B_NAME/tests/*.csv ./
    java -javaagent:../jacoco-0.8.2/lib/jacocoagent.jar -cp ../junit/junit-4.12.jar:../junit/hamcrest-core-1.3.jar:./bin:./bin org.junit.runner.JUnitCore $TEST_NAME
    rm $B_NAME/$TEST_NAME.java
    rm ./bin/$TEST_NAME.class
    rm ./bin/NoExitSecurityManager.class
    rm ./bin/ExitException.class
    java -jar ../jacoco-0.8.2/lib/jacococli.jar report jacoco.exec --classfiles ./bin --sourcefiles $B_NAME --html report
fi
 
