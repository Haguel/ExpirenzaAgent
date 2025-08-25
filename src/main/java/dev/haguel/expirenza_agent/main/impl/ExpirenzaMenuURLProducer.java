package dev.haguel.expirenza_agent.main.impl;

import dev.haguel.expirenza_agent.main.ItemsBuffer;
import dev.haguel.expirenza_agent.main.ItemsBufferProducer;
import org.springframework.stereotype.Component;

@Component
public class ExpirenzaMenuURLProducer extends ItemsBufferProducer<String> {
    public ExpirenzaMenuURLProducer(ItemsBuffer<String> itemsBuffer) {
        super(itemsBuffer);
    }

    @Override
    public void produceItemsToBuffer() {
        itemsBuffer.put("https://expz.menu/edf7ccc5-db00-45f1-b425-e3d1e5ff556a");
    }
}
