package com.bennavetta.aeneas.zookeeper.impl.etcd;

import com.bennavetta.aeneas.zookeeper.ServerRegistryWatcher;
import com.bennavetta.aeneas.zookeeper.ZkServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.requests.EtcdKeyGetRequest;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registry watcher that uses etcd's watch functionality.
 */
public class EtcdServerRegistryWatcher implements ServerRegistryWatcher
{
	private static final Logger LOG = LoggerFactory.getLogger(EtcdServerRegistryWatcher.class);

	private final EtcdClient etcd;
	private final ObjectMapper objectMapper;
	private final Listener listener;

	private final AtomicBoolean running;

	public EtcdServerRegistryWatcher(EtcdClient etcd, Listener listener, ObjectMapper objectMapper)
	{
		this.objectMapper = Preconditions.checkNotNull(objectMapper);
		this.etcd = Preconditions.checkNotNull(etcd);
		this.listener = Preconditions.checkNotNull(listener);

		this.running = new AtomicBoolean(false);
	}

	@Override
	public void start()
	{
		running.compareAndSet(false, true);
		watchOnce(-1);

	}

	@Override
	public void stop()
	{
		running.compareAndSet(true, false);
	}

	private void watchOnce(long index)
	{
		EtcdKeyGetRequest request = etcd.getDir(EtcdServerRegistry.REGISTRY_DIR).recursive();
		if(index == -1)
		{
			request.waitForChange();
		}
		else
		{
			request.waitForChange(index);
		}

		try
		{
			request.send().addListener(promise -> {
				handle(promise.getNow());
				if(running.get())
				{
					watchOnce(promise.getNow().etcdIndex);
				}
			});
		}
		catch (IOException e)
		{
			LOG.error("Error watching for registry changes", e);
		}
	}

	private void handle(EtcdKeysResponse response)
	{
		switch (response.action)
		{
			case set:
			case create:
			case update:
				try
				{
					ZkServer server = objectMapper.readValue(response.node.value, ZkServer.class);
					LOG.debug("Server added: {}", server);
					listener.serverAdded(server);
				}
				catch (IOException e)
				{
					LOG.error("Unable to deserialize server", e);
				}
			case delete:
			case expire:
				int serverId = Integer.parseInt(response.node.key.substring(response.node.key.lastIndexOf('/')));
				LOG.debug("Server removed: {}", serverId);
				listener.serverRemoved(serverId);
			default:
				break;
		}
	}

	@Override
	public boolean isWatching()
	{
		return false;
	}
}