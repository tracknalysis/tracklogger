/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this software except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tracknalysis.tracklogger.dataprovider.ecu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.io.DebugLogWriterIoManager;
import net.tracknalysis.common.io.IoManager;
import net.tracknalysis.common.io.SocketManager;
import net.tracknalysis.common.notification.NotificationListener;
import net.tracknalysis.ecu.ms.DefaultMsConfiguration;
import net.tracknalysis.ecu.ms.DefaultTableManager;
import net.tracknalysis.ecu.ms.Megasquirt;
import net.tracknalysis.ecu.ms.MegasquirtNotificationType;
import net.tracknalysis.ecu.ms.MsConfiguration;
import net.tracknalysis.ecu.ms.TableManager;
import net.tracknalysis.ecu.ms.common.OutputChannel;
import net.tracknalysis.ecu.ms.io.MsIoManager;
import net.tracknalysis.ecu.ms.log.Log;
import net.tracknalysis.tracklogger.dataprovider.AbstractDataProvider;
import net.tracknalysis.tracklogger.dataprovider.EcuDataProvider;
import net.tracknalysis.tracklogger.model.EcuData;

/**
 * @author David Valeri
 */
public class MegasquirtEcuDataProvider extends AbstractDataProvider<EcuData>
        implements EcuDataProvider {
    
    private static final Logger LOG = LoggerFactory.getLogger(MegasquirtEcuDataProvider.class);
    
    private final SocketManager socketManager;
    private final TableManager tableManager;
    private final File debugLogDir;
    private final MsNotificationListener notificationListener = new MsNotificationListener();
    private Megasquirt ms;
    private MsConfiguration msConfig;
    private volatile EcuData currentEcuData;
    private CountDownLatch stopLatch;
    private CountDownLatch startLatch;
    
    public MegasquirtEcuDataProvider(SocketManager socketManager, File debugLogDir) {
        this.socketManager = socketManager;
        this.debugLogDir = debugLogDir;
        tableManager = new DefaultTableManager();
    }

    @Override
    public synchronized void start() {
        if (ms == null) {
        	stopLatch = new CountDownLatch(1);
        	startLatch = new CountDownLatch(1);
        	// Make sure we are connected if not previously connected.
            try {
                msConfig = new DefaultMsConfiguration(
                        new HashSet<String>(Arrays.asList(
                                // Note: We want...
                                //       AFR in AFR (default)
                                //       Pressure in kPa (default)
                                //       Temp in Celsius
                                "CELSIUS")));
                
                IoManager msiom = createIoManager(socketManager);
                msiom.connect();
                
                ms = new Megasquirt(msiom, tableManager, new MegasquirtDataProviderLog(), msConfig, debugLogDir);
                ms.addListener(notificationListener);
                ms.start();
                try {
					startLatch.await();
				} catch (InterruptedException e) {
					//Ignore
				}
            } catch (IOException e) {
                LOG.error("IO error initializing data provider.");
                // TODO
                throw new RuntimeException("IO error initializing data provider.", e);
            }
        }
    }

    @Override
    public synchronized void stop() {
        if (ms != null) {
        	ms.stopLogging();
        	ms.stop();
        	try {
				stopLatch.await();
			} catch (InterruptedException e) {
				//Ignore
			}
        	ms.removeListener(notificationListener);
            ms = null;
        }
    }

    @Override
    public EcuData getCurrentData() {
        return currentEcuData;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
    
    protected IoManager createIoManager(SocketManager socketManager) throws IOException {
    	IoManager msiom = new MsIoManager(socketManager);
        if (debugLogDir != null) {
        	debugLogDir.delete();
        	if (!debugLogDir.exists()) {
                if (!debugLogDir.mkdirs()) {
                	final String message = "Unable to create directory for Megasquirt IO log at " + 
							debugLogDir.getAbsolutePath() + ".";
					LOG.error(message);
					throw new IOException(message);
                }
            }

			msiom = new DebugLogWriterIoManager(msiom, new FileOutputStream(
					new File(debugLogDir, "MegaComIo.log")));
        }
        
        return msiom;
    }
    
    private class MsNotificationListener implements NotificationListener<MegasquirtNotificationType> {

		@Override
		public void onNotification(MegasquirtNotificationType notificationType) {
			onNotification(notificationType, null);
		}

		@SuppressWarnings("incomplete-switch")
		@Override
		public void onNotification(MegasquirtNotificationType notificationType,
				Object messageBody) {
			switch (notificationType) {
				case CONNECTED:
					startLatch.countDown();
					ms.startLogging();
					break;
				case DISCONNECTED:
					startLatch.countDown();
					stopLatch.countDown();
					break;
			}
		}
    }
    
    private class MegasquirtDataProviderLog implements Log {
        
        private volatile boolean logging = false;
        private volatile long startTime;
        
        private OutputChannel afr;
        private OutputChannel batV;
        private OutputChannel clt;
        private OutputChannel ignAdv;
        private OutputChannel map;
        private OutputChannel boostvac;
        private OutputChannel mat;
        private OutputChannel rpm;
        private OutputChannel tps;
        private boolean firstWrite = true;

        @Override
        public synchronized void start() throws IOException {
            logging = true;
            startTime = System.currentTimeMillis();
            
            LOG.debug("Started logging.");
        }

        @Override
        public synchronized void stop() throws IOException {
            logging = false;
            
            LOG.debug("Stopped logging.");
        }

        @Override
        public void mark(String message) throws IOException {
            // NoOp
        }

        @Override
        public void mark() throws IOException {
            // NoOp
        }

        @Override
        public boolean isMarkSupported() {
            return false;
        }

        @Override
        public long getStartTime() {
            return startTime;
        }

        @Override
        public void write(Megasquirt ms) throws IOException {
        	if (logging) {
	            LOG.debug("Received new Megasquirt update.  Refreshing current ECU data.");
	            
	            if (firstWrite) {
	            	afr = ms.getOutputChannelByName("afr1");
	            	batV = ms.getOutputChannelByName("batteryVoltage");
	            	clt = ms.getOutputChannelByName("coolant");
	            	ignAdv = ms.getOutputChannelByName("advance");
	            	map = ms.getOutputChannelByName("map");
	            	boostvac = ms.getOutputChannelByName("boostvac");
	            	mat = ms.getOutputChannelByName("mat");
	            	rpm = ms.getOutputChannelByName("rpm");
	            	tps = ms.getOutputChannelByName("throttle");
	            	
	            }
	            
	            
				EcuData.EcuDataBuilder builder = new EcuData.EcuDataBuilder();
				builder.setDataRecivedTime(System.currentTimeMillis());
				builder.setAirFuelRatio(getDoubleIfAvailable(afr));
				builder.setBatteryVoltage(getDoubleIfAvailable(batV));
				builder.setCoolantTemperature(getDoubleIfAvailable(clt));
				builder.setIgnitionAdvance(getDoubleIfAvailable(ignAdv));
				builder.setManifoldAbsolutePressure(getDoubleIfAvailable(map));
				builder.setManifoldGaugePressure(getDoubleIfAvailable(boostvac) * 6.89475729d); // PSI to KPa
				builder.setManifoldAirTemperature(getDoubleIfAvailable(mat));
				builder.setRpm(getIntIfAvailable(rpm));
	            
	            double throttle = getDoubleIfAvailable(tps);
	            // Set the floor for throttle position to 0
	            if (throttle < 0d) {
	                throttle = 0d;
	            }
	            
	            builder.setThrottlePosition(throttle);
	            
	            EcuData newEcuData = builder.build();
	            currentEcuData = newEcuData;
	            
	            notifySynchronousListeners(newEcuData);
        	}
        }

        @Override
        public synchronized boolean isLogging() {
            return logging;
        }
        
        private double getDoubleIfAvailable(OutputChannel outputChannel) {
        	if (outputChannel != null) {
        		return outputChannel.getValue();
        	} else {
        		return 0d;
        	}
        }
        
        private int getIntIfAvailable(OutputChannel outputChannel) {
        	if (outputChannel != null) {
        		return (int) outputChannel.getValue();
        	} else {
        		return 0;
        	}
        }
    }
}
