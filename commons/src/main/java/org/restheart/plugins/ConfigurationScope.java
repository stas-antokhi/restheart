/*-
 * ========================LICENSE_START=================================
 * restheart-commons
 * %%
 * Copyright (C) 2019 - 2022 SoftInstigate
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.restheart.plugins;

/**
 * Defines the scope of the configuration to inject via @OnInit
 *
 * ALL injects the whole configuration
 *
 * OWN injects the configuration of the plugins identified by its name
 *
 * @author Andrea Di Cesare {@literal <andrea@softinstigate.com>}
 */
public enum ConfigurationScope {
    ALL, OWN
}
