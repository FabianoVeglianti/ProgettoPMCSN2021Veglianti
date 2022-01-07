package reteDiCode;

public class NetworkConfiguration {

    private enum EC2InstanceType {T2Nano, T2Micro, T2Small}

    private class EC2Instance{
        private static final double MEAN_VM1_SERVICE_TIME_T2NANO = 1.0/15.0;
        private static final double MEAN_VM1_SERVICE_TIME_T2MICRO = 1.0/20.0;
        private static final double MEAN_VM1_SERVICE_TIME_T2SMALL = 1.0/30.0;
        private static final double MEAN_VM2CPU_SERVICE_TIME_T2NANO = 1.0/12.0;
        private static final double MEAN_VM2CPU_SERVICE_TIME_T2MICRO = 1.0/15.0;
        private static final double MEAN_VM2CPU_SERVICE_TIME_T2SMALL = 1.0/22.0;
        private static final double MEAN_VM2BAND_SERVICE_TIME_T2NANO = 1.0/4.0;
        private static final double MEAN_VM2BAND_SERVICE_TIME_T2MICRO = 1.0/7.0;
        private static final double MEAN_VM2BAND_SERVICE_TIME_T2SMALL = 1.0/9.0;

        public double getPricePerMinute(){
            switch (this.type){
                case T2Nano:
                    return 0.01;
                case T2Micro:
                    return 0.02;
                case T2Small:
                    return 0.05;
                default:
                    System.err.println("ERRORE FATALE");
                    System.exit(-1);
                    return -1;
            }
        }

        private EC2InstanceType type;

        public EC2Instance(EC2InstanceType type){
            this.type = type;
        }

        public double getMeanVm1ServiceTime(){
            switch (this.type){
                case T2Nano:
                    return MEAN_VM1_SERVICE_TIME_T2NANO;
                case T2Micro:
                    return MEAN_VM1_SERVICE_TIME_T2MICRO;
                case T2Small:
                    return MEAN_VM1_SERVICE_TIME_T2SMALL;
                default:
                    System.err.println("ERRORE FATALE");
                    System.exit(-1);
                    return -1;
            }
        }

        public double getMeanVm2cpuServiceTime(){
            switch (this.type){
                case T2Nano:
                    return MEAN_VM2CPU_SERVICE_TIME_T2NANO;
                case T2Micro:
                    return MEAN_VM2CPU_SERVICE_TIME_T2MICRO;
                case T2Small:
                    return MEAN_VM2CPU_SERVICE_TIME_T2SMALL;
                default:
                    System.err.println("ERRORE FATALE");
                    System.exit(-1);
                    return -1;
            }
        }

        public double getMeanVm2bandServiceTime(){
            switch (this.type){
                case T2Nano:
                    return MEAN_VM2BAND_SERVICE_TIME_T2NANO;
                case T2Micro:
                    return MEAN_VM2BAND_SERVICE_TIME_T2MICRO;
                case T2Small:
                    return MEAN_VM2BAND_SERVICE_TIME_T2SMALL;
                default:
                    System.err.println("ERRORE FATALE");
                    System.exit(-1);
                    return -1;
            }
        }
    }

    private EC2Instance vm1Instance;
    private EC2Instance vm2Instance;

    public void setConfiguration(char configurationCode){
        switch (configurationCode){
            case 'A':
                vm1Instance = new EC2Instance(EC2InstanceType.T2Nano);
                vm2Instance = new EC2Instance(EC2InstanceType.T2Nano);
                break;
            case 'B':
                vm1Instance = new EC2Instance(EC2InstanceType.T2Nano);
                vm2Instance = new EC2Instance(EC2InstanceType.T2Micro);
                break;
            case 'C':
                vm1Instance = new EC2Instance(EC2InstanceType.T2Nano);
                vm2Instance = new EC2Instance(EC2InstanceType.T2Small);
                break;
            case 'D':
                vm1Instance = new EC2Instance(EC2InstanceType.T2Micro);
                vm2Instance =  new EC2Instance(EC2InstanceType.T2Nano);
                break;
            case 'E':
                vm1Instance = new EC2Instance(EC2InstanceType.T2Micro);
                vm2Instance = new EC2Instance(EC2InstanceType.T2Micro);
                break;
            case 'F':
                vm1Instance =  new EC2Instance(EC2InstanceType.T2Micro);
                vm2Instance = new EC2Instance(EC2InstanceType.T2Small);
                break;
            case 'G':
                vm1Instance =new EC2Instance(EC2InstanceType.T2Small);
                vm2Instance = new EC2Instance(EC2InstanceType.T2Nano);
                break;
            case 'H':
                vm1Instance = new EC2Instance(EC2InstanceType.T2Small);
                vm2Instance = new EC2Instance(EC2InstanceType.T2Micro);
                break;
            case 'I':
                vm1Instance = new EC2Instance(EC2InstanceType.T2Small);
                vm2Instance = new EC2Instance(EC2InstanceType.T2Small);
                break;
            default:
                System.err.println("ERRORE FATALE");
                System.exit(-1);
                break;
        }

        Params.MEAN_SERVICE_TIME_VM1 = vm1Instance.getMeanVm1ServiceTime();
        Params.MEAN_SERVICE_TIME_VM2CPU = vm2Instance.getMeanVm2cpuServiceTime();
        Params.MEAN_SERVICE_TIME_VM2BAND = vm2Instance.getMeanVm2bandServiceTime();

        Params.VM1_PRICE_PER_MINUTE = vm1Instance.getPricePerMinute();
        Params.VM2CPU_PRICE_PER_MINUTE = vm2Instance.getPricePerMinute();
        Params.VM2BAND_PRICE_PER_MINUTE = vm2Instance.getPricePerMinute();

    }

    private static NetworkConfiguration instance = null;

    private NetworkConfiguration(){}

    public static NetworkConfiguration getInstance() {
        if(instance == null) {
            instance = new NetworkConfiguration();
        }
        return instance;
    }
}
