import org.junit.Test;
import org.junit.Rule;
import junit.framework.TestCase;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Collections;
import java.util.Arrays;

import static org.junit.Assert.*;



public class IOCoverageTest extends TestCase {




    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        System.setSecurityManager(new NoExitSecurityManager());
    }

    @Override
    protected void tearDown() throws Exception
    {
        System.setSecurityManager(null); // or save and restore original
        super.tearDown();
    }

    public void testWithExit() throws Exception
    {
        try
        {
            runCoverage();
        } catch (ExitException e)
        {
            assertEquals("Exit status", 0, e.status);
        }
    }

    public void removeUseless(List<File> in, List<File> out) {
        for(int i = 0; i < in.size(); i++) {
            boolean match = false;
            for(int j = 0; j < out.size(); j++) {
                if(in.get(i).getName().substring(0, in.get(i).getName().length()-3).equals(
                    out.get(j).getName().substring(0, out.get(j).getName().length()-4))) {
                        match = true;
                        break;
                    }
            }

            if(!match) {
                in.remove(i);
                i--;
            }
        }

        for(int i = 0; i < out.size(); i++) {
            boolean match = false;
            for(int j = 0; j < in.size(); j++) {
                if(in.get(i).getName().substring(0, in.get(i).getName().length()-3).equals(
                    out.get(j).getName().substring(0, out.get(j).getName().length()-4))) {
                        match = true;
                        break;
                    }
            }

            if(!match) {
                out.remove(i);
                i--;
            }
        }
    }

    @Test
    public void runCoverage() {
        File folder = new File("./tests/");
        List<File> files = new ArrayList<File>();
        files.addAll(Arrays.asList(folder.listFiles()));
        List<File> inPaths = new ArrayList<>();
        List<File> outPaths = new ArrayList<>();
        List<String> flaggedTests = new ArrayList<>();

        //Extract files
        for(int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            if(f.isDirectory()) {
                files.addAll(Arrays.asList(f.listFiles()));

            } else if(f.isFile()) {
                if(f.getName().endsWith(".in")) {
                    inPaths.add(f);
                } else if(f.getName().endsWith(".out")) {
                    outPaths.add(f);
                }
            }
        }

        removeUseless(inPaths, outPaths);

        Collections.sort(inPaths);
        Collections.sort(outPaths);



        if(inPaths.size() == outPaths.size()) {
            for(int i = 0; i < inPaths.size(); i++) {
                System.out.println("\n" + inPaths.size() + " : " + inPaths.get(i).getName());
                if(!coverage(inPaths.get(i).getPath(), outPaths.get(i).getPath())) {

                    flaggedTests.add(inPaths.get(i).getName().substring(0, inPaths.get(i).getName().length()-3));
                }
            }
        } else {
            System.out.println("Something broke!");
        }

        for(String s : flaggedTests) {
            System.out.println("This test has been flagged for input differences: " + s);
        }

    }


    public boolean coverage(String inputFile, String expectedOutFile) {
	      /*inputFile = "./tests/" + inputFile;
        expectedOutFile = "./tests/" + expectedOutFile;*/
        PrintStream originalOut = System.out;
        InputStream originalIn = System.in;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream customOutStream = new PrintStream(outputStream);
        InputStream customInStream = null;
        try {
            customInStream = new FileInputStream(inputFile);
            System.setOut(customOutStream);
            System.setIn(customInStream);
        } catch (SecurityException e) {
            return false;
        } catch (FileNotFoundException e) {
            return false;
        }

        try
        {
            FlightScheduler.main(new String[0]);
        } catch (ExitException e)
        {
            assertEquals("Exit status", 0, e.status);
        }


        System.setOut(originalOut);
        System.setIn(originalIn);
        String outputMsg = outputStream.toString();
        try {
            customInStream.close();
            customOutStream.close();
            outputStream.close();
        } catch (IOException e) {

        }
        try {
            Scanner outputReader = new Scanner(outputMsg);
            Scanner expectedReader = new Scanner(new File(expectedOutFile));

            while (expectedReader.hasNextLine() && outputReader.hasNextLine()) {
                String expected = expectedReader.nextLine();
                String actual = outputReader.nextLine();
                if(!expected.equals(actual)) {
                    return false;
                }
            }

            if (outputReader.hasNextLine()) {
                return false;
            } else if (expectedReader.hasNextLine()) {
                String line = expectedReader.nextLine();
                return false;
            }

            outputReader.close();
            expectedReader.close();

        } catch (FileNotFoundException e) {
            return false;
        }

        return true;
    }


}
