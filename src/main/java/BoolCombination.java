/*
 * Copyright 2021 InfAI (CC SES)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import org.infai.ses.senergy.operators.BaseOperator;
import org.infai.ses.senergy.operators.Helper;
import org.infai.ses.senergy.operators.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BoolCombination extends BaseOperator {

    private final Map<String, String> results = new HashMap<>();
    private final Map<String, Boolean> tiltStates = new HashMap<>();
    private final Map<String, String> windowStates = new HashMap<>();

    final String windowOpen = "Door/Window Open";
    final String windowClosed = "Door/Window Closed";
    final int deviceIdLength = 57;

    final boolean debug = Boolean.parseBoolean(Helper.getEnv("DEBUG", "false"));

    @Override
    public Message configMessage(Message message) {
        message.addFlexInput("windowStatus");
        message.addFlexInput("tiltStatus");
        return message;
    }

    @Override
    public void run(Message message) {
        message.output("status", null); // If not overwritten, old values might be used
        message.output("device", null);
        Set<Map.Entry<String, String>> windowEntries = message.getFlexInput("windowStatus").getFilterIdValueMap(String.class).entrySet();
        Set<Map.Entry<String, String>> tiltEntries = message.getFlexInput("tiltStatus").getFilterIdValueMap(String.class).entrySet();

        String[] devices = new String[windowEntries.size() + tiltEntries.size()];
        int i = 0;
        for (Map.Entry<String, String> entr : windowEntries) {
            String device = getDeviceIdPartOfFilter(entr.getKey());
            devices[i] = device;
            windowStates.put(device, entr.getValue());
            i++;
        }
        for (Map.Entry<String, String> entr : tiltEntries) {
            String device = getDeviceIdPartOfFilter(entr.getKey());
            devices[i] = device;
            tiltStates.put(device, Boolean.parseBoolean(entr.getValue()));
            i++;
        }

        if (debug) {
            System.out.println("Got messages from devices");
            for (String d : devices)
                System.out.println(d);
        }

        for (String device : devices) {
            String status;
            String windowStatus = windowStates.get(device);
            Boolean tiltStatus = tiltStates.get(device);
            if (windowStatus == null) {
                if (debug)
                    System.out.println("Device has no windowState yet: " + device);
                continue;
            }
            if (tiltStatus == null) {
                if (debug)
                    System.out.println("Device has no tiltState yet: " + device);
                continue;
            }
            try {
                status = calculateStatus(windowStatus, tiltStatus);
            } catch (InvalidStatusException e) {
                System.err.println("Invalid state encountered, message ignored. Device: " + device);
                continue;
            }
            if (!results.containsKey(device) || !results.get(device).equals(status)) {
                results.put(device, status);
                message.output("status", status);
                message.output("device", device);
                if (debug)
                    System.out.println("Status: " + status + ", Device: " + device);
                return;
            }
        }
    }

    private String calculateStatus(String windowStatus, boolean tiltStatus) throws InvalidStatusException {
        if (windowStatus.equals(windowOpen) && tiltStatus) {
            return "tilted";
        } else if (windowStatus.equals(windowOpen) && !tiltStatus) {
            return "opened";
        } else if (windowStatus.equals(windowClosed) && tiltStatus) {
            throw new InvalidStatusException();
        } else if (windowStatus.equals(windowClosed) && !tiltStatus) {
            return "closed";
        }
        throw new InvalidStatusException();
    }

    private String getDeviceIdPartOfFilter(String filter) {
        return filter.substring(0, Math.min(deviceIdLength, filter.length()));
    }
}
