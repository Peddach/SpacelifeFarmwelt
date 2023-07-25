package de.petropia.farmworld;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.channel.ChannelMessage;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;

public class FarmworldCLMessageListener {

    @EventListener
    public void onStatusQueryMessage(ChannelMessageReceiveEvent event){
        if(!event.getChannel().equals("farmworld_status")){
            return;
        }
        if(!event.isQuery()){
            return;
        }
        ChannelMessage message = event.getChannelMessage();
        if(!message.getMessage().equals("world_status")){
            return;
        }
        event.setQueryResponse(ChannelMessage.buildResponseFor(message).json(JsonDocument.newDocument()
                .append("available", Farmworld.isAvailable())
                .append("delete", Farmworld.getNextDelete().getEpochSecond()))
                .build());
    }
}
