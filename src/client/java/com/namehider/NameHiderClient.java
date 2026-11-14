package com.namehider;

import net.fabricmc.api.ClientModInitializer;

public class NameHiderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NHLogFilter.install();
    }
}
