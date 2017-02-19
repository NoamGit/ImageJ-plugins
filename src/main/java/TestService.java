
//TestService.java
import net.imagej.ImageJ;
import net.imagej.ImageJService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

public interface TestService extends ImageJService {

    public void sayHi();

    @Plugin(type = Command.class, headless = true)
    class Test implements Command {

        @Parameter
        private TestService testService;

        @Parameter
        private LogService log;

        @Override
        public void run() {

            testService.sayHi();

        }

        public static void main(final String... args) throws Exception {
            System.out.println("Starting test");
            // Launch ImageJ as usual.
            final ImageJ ij = net.imagej.Main.launch(args);
            ij.update();
//            ij.command().run(Test.class, true);
        }

    }
}