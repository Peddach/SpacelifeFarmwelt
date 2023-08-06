package de.petropia.farmworld.listener;

import de.petropia.farmworld.Farmworld;
import eu.cloudnetservice.driver.channel.ChannelMessage;
import eu.cloudnetservice.driver.event.EventListener;
import eu.cloudnetservice.driver.event.events.channel.ChannelMessageReceiveEvent;
import eu.cloudnetservice.driver.network.buffer.DataBuf;

public class FarmworldCLMessageListener {

    @EventListener
    public void onStatusQueryMessage(ChannelMessageReceiveEvent event){
        if(!event.channel().equals("farmworld_status")){
            return;
        }
        if(!event.query()){
            return;
        }
        ChannelMessage message = event.channelMessage();
        if(!message.message().equals("world_status")){
            return;
        }
        event.queryResponse(ChannelMessage.buildResponseFor(message).buffer(DataBuf.empty()
                        .writeBoolean(Farmworld.isAvailable())
                        .writeLong(Farmworld.getNextDelete().getEpochSecond()))
                .build());
    }
}
