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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.tracknalysis.common.io.SocketManager;
import net.tracknalysis.ecu.ms.DefaultMegasquirtConfiguration;
import net.tracknalysis.ecu.ms.DefaultTableManager;
import net.tracknalysis.ecu.ms.Megasquirt;
import net.tracknalysis.ecu.ms.MegasquirtConfiguration;
import net.tracknalysis.ecu.ms.MegasquirtFactory;
import net.tracknalysis.ecu.ms.MegasquirtFactoryException;
import net.tracknalysis.ecu.ms.SignatureException;
import net.tracknalysis.ecu.ms.TableManager;
import net.tracknalysis.ecu.ms.io.DirectMegasquirtIoManager;
import net.tracknalysis.ecu.ms.io.MegasquirtIoManager;
import net.tracknalysis.ecu.ms.log.Log;
import net.tracknalysis.tracklogger.dataprovider.AbstractDataProvider;
import net.tracknalysis.tracklogger.dataprovider.EcuData;
import net.tracknalysis.tracklogger.dataprovider.EcuDataProvider;

/**
 * @author David Valeri
 */
public class MegasquirtEcuDataProvider extends AbstractDataProvider<EcuData>
        implements EcuDataProvider {
    
    private static final Logger LOG = LoggerFactory.getLogger(MegasquirtEcuDataProvider.class);
    
    private final SocketManager socketManager;
    private final TableManager tableManager;
    private Megasquirt ms;
    private MegasquirtConfiguration msConfig;
    private volatile EcuData currentEcuData;
    
    public MegasquirtEcuDataProvider(SocketManager socketManager) {
        this.socketManager = socketManager;
        tableManager = new DefaultTableManager();
    }

    @Override
    public synchronized void start() {
        if (ms == null) {
            // Make sure we are connected if not previously connected.
            try {
                socketManager.connect();
                
                MegasquirtIoManager msiom = new DirectMegasquirtIoManager(socketManager);
                msConfig = new DefaultMegasquirtConfiguration(
                        new HashSet<String>(Arrays.asList(
                                // Note: We want...
                                //       AFR in AFR (default)
                                //       Pressure in kPa (default)
                                //       Temp in Celsius
                                "CELSIUS")));
                
                ms = MegasquirtFactory.getInstance().getMegasquirt(
                        msiom, 
                        tableManager,
                        new MegasquirtDataProviderLog(),
                        msConfig);
                ms.start();
                ms.startLogging();
            } catch (IOException e) {
                LOG.error("IO error initializing data provider.");
                // TODO
                throw new RuntimeException("IO error initializing data provider.", e);
            } catch (MegasquirtFactoryException e) {
                LOG.error("Error initializing Megasquirt communication sub-system.");
                // TODO
                throw new RuntimeException(
                        "Error initializing Megasquirt communication sub-system.",
                        e);
            } catch (SignatureException e) {
                LOG.error("Error determining Megasquirt signature.");
                // TODO
                throw new RuntimeException(
                        "Error determining Megasquirt signature.",
                        e);
            }
        }
    }

    @Override
    public synchronized void stop() {
        if (ms != null) {
            ms.stop();
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
    
    protected class MegasquirtDataProviderLog implements Log {
        
        boolean logging = false;
        private volatile long startTime;

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
            LOG.debug("Received new Megasquirt update.  Refreshing current ECU data.");
            
            EcuData.EcuDataBuilder builder = new EcuData.EcuDataBuilder();
            builder.setDataRecivedTime(System.currentTimeMillis());
            builder.setAirFuelRatio(ms.getValue("afr1"));
            builder.setBatteryVoltage(ms.getValue("batteryVoltage"));
            builder.setCoolantTemperature(ms.getValue("coolant"));
            builder.setIgnitionAdvance(ms.getValue("advance"));
            builder.setManifoldAbsolutePressure(ms.getValue("map"));
            builder.setManifoldAirTemperature(ms.getValue("mat"));
            builder.setRpm((int) ms.getValue("rpm"));
            builder.setThrottlePosition(ms.getValue("throttle"));
            
            EcuData newEcuData = builder.build();
            currentEcuData = newEcuData;
            
            notifySynchronousListeners(newEcuData);
        }

        @Override
        public boolean isLogging() {
            return false;
        }
    }
}
