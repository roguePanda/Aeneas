/**
 * Copyright 2014 Benjamin Navetta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bennavetta.aeneas.zookeeper.node;

import java.io.IOException;
import java.nio.file.Paths;

import mousio.etcd4j.EtcdClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bennavetta.aeneas.Etcd;
import com.bennavetta.aeneas.zookeeper.IdGenerator;
import com.bennavetta.aeneas.zookeeper.ServerRegistry;
import com.bennavetta.aeneas.zookeeper.ZkException;
import com.bennavetta.aeneas.zookeeper.impl.etcd.EtcdIdGenerator;
import com.bennavetta.aeneas.zookeeper.impl.etcd.EtcdServerRegistry;

/**
 * Launch and register a ZooKeeper node
 */
public class Main
{
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args)
	{
		EtcdClient etcd = Etcd.createClient();

		LOG.info("Connected to etcd - {}", etcd.getVersion());

		ServerRegistry registry = new EtcdServerRegistry(etcd);
		IdGenerator idGenerator = new EtcdIdGenerator(etcd);
		ZooKeeper zookeeper = new ZooKeeper(Paths.get(System.getenv().getOrDefault("ZOO_DIR", "/opt/zookeeper")),
		                                    registry, idGenerator);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try
			{
				zookeeper.deregister();
				zookeeper.kill();
				etcd.close();
			}
			catch (ZkException e)
			{
				LOG.error("Error deregistering ZooKeeper node", e);
			}
			catch (IOException e)
			{
				LOG.error("Error closing etcd connection", e);
			}
		}));

		try
		{
			zookeeper.configure();
			LOG.debug("Configured ZooKeeper");
		}
		catch (IOException | ZkException e)
		{
			LOG.error("Error configuring ZooKeeper", e);
			System.exit(1);
		}

		try
		{
			zookeeper.launch();
			LOG.debug("Launched ZooKeeper");
		}
		catch (IOException | ZkException e)
		{
			LOG.error("Error starting ZooKeeper", e);
			System.exit(1);
		}

		try
		{
			int exit = zookeeper.waitFor();
			LOG.info("ZooKeeper exited with code {}", exit);
		}
		catch (InterruptedException e)
		{
			LOG.error("Error waiting for ZooKeeper to complete", e);
			System.exit(1);
		}
	}
}
