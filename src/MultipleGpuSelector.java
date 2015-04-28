
import com.amd.aparapi.Range;
import com.amd.aparapi.device.Device;
import com.amd.aparapi.device.OpenCLDevice;
import com.amd.aparapi.device.OpenCLDevice.DeviceComparitor;
import com.amd.aparapi.device.OpenCLDevice.DeviceSelector;

public abstract class MultipleGpuSelector {

    public static Device worst() {
        return OpenCLDevice.select(new DeviceComparitor() {
            public OpenCLDevice select(OpenCLDevice _deviceLhs, OpenCLDevice _deviceRhs) {
                return _deviceLhs.getType() != _deviceRhs.getType() ? (_deviceLhs.getType() == Device.TYPE.GPU ? _deviceLhs : _deviceRhs) : (_deviceLhs.getMaxComputeUnits() < _deviceRhs.getMaxComputeUnits() ? _deviceLhs : _deviceRhs);
            }
        });
    }
}
