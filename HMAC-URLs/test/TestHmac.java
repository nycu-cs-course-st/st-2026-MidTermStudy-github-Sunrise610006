import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import smrl.mr.crawljax.WebOperationsProvider;
import smrl.mr.language.MRBaseTest;
import smrl.mr.hmac.HmacResourceBinding;

public class TestHmac extends MRBaseTest {

    private static WebOperationsProvider provider;

    // create data provider before execution
    @BeforeClass
    public static void setUpBeforeClass() {
        // read settings of test of HMAC Web Server
        provider = new WebOperationsProvider("./data/config.json");
    }

    // set provider before a test
    @Before
    public void setUp() {
        // provide Data Provider settings to test framework
        setProvider(provider);
    }

    // tell JUnit that this is a test case
    @Test
    public void testResourceBinding() {
        // execute self-designed MR
        super.test(provider, HmacResourceBinding.class);
    }
}
