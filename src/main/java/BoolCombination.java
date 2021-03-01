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

import org.infai.ses.senergy.exceptions.NoValueException;
import org.infai.ses.senergy.operators.BaseOperator;
import org.infai.ses.senergy.operators.Message;

public class BoolCombination extends BaseOperator {

    final String windowOpen = "Door/Window Open";
    final String windowClosed = "Door/Window Closed";


    @Override
    public Message configMessage(Message message){
        message.addFlexInput("windowStatus");
        message.addFlexInput("tiltStatus");
        return message;
    }

    @Override
    public void run(Message message) {
        String windowStatus = "";
        boolean tiltStatus = false;
        String status = null;
        String device = null;
        try {
            windowStatus = message.getFlexInput("windowStatus").getString();
            tiltStatus = Boolean.parseBoolean(message.getFlexInput("tiltStatus").getString());
            device = message.getFlexInput("windowStatus").getCurrentFilterId();
        } catch (NoValueException e) {
            e.printStackTrace();
        }
        if (windowStatus.equals(windowOpen) && tiltStatus){
            status="tilted";
        } else if (windowStatus.equals(windowOpen) && !tiltStatus){
            status="opened";
        } else if (windowStatus.equals(windowClosed) && tiltStatus){
            return;
        } else if (windowStatus.equals(windowClosed) && !tiltStatus){
            status="closed";
        }

        message.output("status", status);
        message.output("device", device);
    }
}