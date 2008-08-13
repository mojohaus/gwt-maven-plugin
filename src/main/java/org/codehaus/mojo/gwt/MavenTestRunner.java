package org.codehaus.mojo.gwt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.maven.surefire.report.BriefConsoleReporter;
import org.apache.maven.surefire.report.FileReporter;
import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.Reporter;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.report.XMLReporter;

/**
 * Mostly a copy/paste of surefire TestListenerInvocationHandler
 *
 * @author ndeloof
 */
public class MavenTestRunner
    extends TestRunner
{
    ReporterManager reportManager;

    boolean testHadFailed;

    public static void main( String args[] )
    {
        try
        {
            MavenTestRunner runner = new MavenTestRunner();
            TestResult r = runner.start( args );
            if ( !r.wasSuccessful() )
                System.exit( FAILURE_EXIT );
            System.exit( SUCCESS_EXIT );
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
            System.err.println( t.getMessage() );
            System.exit( EXCEPTION_EXIT );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see junit.textui.TestRunner#createTestResult()
     */
    @Override
    protected TestResult createTestResult()
    {
        TestResult result = super.createTestResult();
        result.addListener( this );
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see junit.textui.TestRunner#doRun(junit.framework.Test, boolean)
     */
    @Override
    public TestResult doRun( Test suite, boolean wait )
    {
        try
        {
            reportManager.runStarting( suite.countTestCases() );
            ReportEntry report = new ReportEntry( this.getClass().getName(), suite.toString(), "starting" );
            reportManager.testSetStarting( report );
            TestResult result = createTestResult();
            suite.run( result );
            return result;
        }
        catch ( ReporterException e )
        {
            System.err.println( "Failed to log in test report " + e );
            return null;
        }
        finally
        {
            ReportEntry report = new ReportEntry( this.getClass().getName(), suite.toString(), "ended" );
            reportManager.testSetCompleted( report );
            reportManager.runCompleted();
        }
    }

    /**
     *
     */
    public MavenTestRunner()
    {
        String dir = System.getProperty( "surefire.reports" );
        List<Reporter> reports = new ArrayList<Reporter>();
        reports.add( new XMLReporter( new File( dir ), false ) );
        reports.add( new FileReporter( new File( dir ), false ) );
        reports.add( new BriefConsoleReporter( true ) );
        reportManager = new ReporterManager( reports );

    }

    /**
     * A test started.
     */
    public void startTest( Test test )
    {
        testHadFailed = false;
        ReportEntry report = new ReportEntry( test.getClass().getName(), test.toString(), test.getClass().getName() );
        reportManager.testStarting( report );
    }

    /**
     * A test ended.
     */
    public void endTest( Test test )
    {
        if ( !testHadFailed )
        {
            ReportEntry report =
                new ReportEntry( test.getClass().getName(), test.toString(), test.getClass().getName() );
            reportManager.testSucceeded( report );
        }
    }

    /**
     * An error occurred.
     */
    public void addError( Test test, Throwable t )
    {
        ReportEntry report =
            new ReportEntry( test.getClass().getName(), test.toString(), test.toString(), getStackTraceWriter( test, t ) );

        reportManager.testError( report );
        testHadFailed = true;
    }

    /**
     * A failure occurred.
     */
    public void addFailure( Test test, AssertionFailedError t )
    {
        ReportEntry report =
            new ReportEntry( test.getClass().getName(), test.toString(), test.toString(), getStackTraceWriter( test, t ) );

        reportManager.testFailed( report );
        testHadFailed = true;
    }

    private StackTraceWriter getStackTraceWriter( Test test, Throwable t )
    {
        String name = test.getClass().getName();
        String testName = "UNKNOWN";
        if ( test instanceof TestSuite )
        {
            testName = ( (TestSuite) test ).getName();
        }
        return new PojoStackTraceWriter( name, testName, t );
    }
}
