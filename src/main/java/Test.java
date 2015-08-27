//Test.java
import net.imagej.ImageJ;
import net.imagej.Dataset;
import java.lang.System;

import org.scijava.log.LogService;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import org.scijava.service.Service;
import net.imagej.DatasetService;


@Plugin(type = Command.class, headless = true)
public class Test implements Command {

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

        ij.command().run(Test.class, true);
    }

}