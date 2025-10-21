package com.tabii.data.transformers.pgToHazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.HazelcastProperties;
import com.tabii.utils.HcMaps;

public class TestMaps {

	public static void main(String[] args) {
		HazelcastProperties hzp = CommonUtils.getHazelcastConnectionProps();
		ClientConfig config = new ClientConfig();
		config.setClusterName(hzp.getClusterName());
		config.getNetworkConfig().setAddresses(CommonUtils.serversSplitter(hzp.getServers()));
		HazelcastInstance hzi = HazelcastClient.newHazelcastClient(config);

		HcMaps[] mapNames = HcMaps.values();

		for (HcMaps mapKey : mapNames) {
			String mapName = mapKey.getMapName();
			System.out.print("Checking map " + mapName + " ... ");
			IMap<String, String> map = hzi.getMap(mapName);
			System.out.println("✅ Size of the map " + map.getName() + " : " + map.size());
		}

		showsTest(hzi);
		
		imagesTest(hzi);
		
		hzi.shutdown();
	}
	
	private static void showsTest(HazelcastInstance hzi) {
		IMap<String, String> map = hzi.getMap("showsMap");

		// [show:524755, show:155856, show:154520, show:154554, show:191180, show:155919]
		
		String sampleKey = "show:524755";
		sampleKey = "show:191180";
		
		String sampleValue = map.get(sampleKey);
		System.out.println("Sample key: " + sampleKey);
		System.out.println("Sample value: " + sampleValue);

	}
	
	private static void imagesTest(HazelcastInstance hzi) {
		
		IMap<String, String> map = hzi.getMap("imagesMap");
		String sampleKey = "image:2266";
		
		String sampleValue = map.get(sampleKey);
		System.out.println("Sample key: " + sampleKey);
		System.out.println("Sample value: " + sampleValue);
	}

}
