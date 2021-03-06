/*
 *    Copyright [2022] [brick-team]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.brick.action.flow.web.common.config;

import com.github.brick.action.flow.method.config.ActionFlowVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author Zen Huifer
 */
@Component
public class ActionFlowLogRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(ActionFlowLogRunner.class);

    @Override
    public void run(String... args) throws Exception {
        logger.info("Action Flow Version: {}", ActionFlowVersion.VERSION);




    }
}
