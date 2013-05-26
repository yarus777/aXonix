/*
 * Copyright 2012-2013 Ivan Gadzhega
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package net.ivang.axonix.main;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import net.ivang.axonix.main.screen.StartScreen;
import net.ivang.axonix.main.screen.game.GameScreen;
import net.ivang.axonix.main.screen.levels.LevelsScreen;

/**
 * @author Ivan Gadzhega
 * @since 0.1
 */
public class AxonixModule extends AbstractModule {

    @Override
    protected void configure() {
        // Game
        bind(AxonixGame.class).in(Singleton.class);
        // Screens
        bind(StartScreen.class).in(Singleton.class);
        bind(LevelsScreen.class).in(Singleton.class);
        bind(GameScreen.class).in(Singleton.class);
        // Event Bus
        bind(EventBus.class).in(Singleton.class);
    }

}