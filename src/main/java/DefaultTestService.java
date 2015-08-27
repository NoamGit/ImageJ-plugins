// DefaultTestService.java
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Service.class)
public class DefaultTestService extends AbstractService implements TestService {

    @Parameter
    LogService log;

    @Override
    public void sayHi(){
        log.error("Hi");
    }
}