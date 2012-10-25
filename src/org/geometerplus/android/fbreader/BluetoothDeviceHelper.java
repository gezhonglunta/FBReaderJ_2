package org.geometerplus.android.fbreader;

import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class BluetoothDeviceHelper {
	private static BluetoothDeviceHelper instance;
	BluetoothAdapter adapter;

	public static BluetoothDeviceHelper Instance() {
		if (instance == null) {
			instance = new BluetoothDeviceHelper();
		}
		return instance;
	}

	public BluetoothDeviceHelper() {
		adapter = BluetoothAdapter.getDefaultAdapter();
	}

	public boolean isEnable() {
		if (adapter == null) {
			return false;
		}
		return adapter.isEnabled();
	}

	public boolean hasBluetoothMouse() {
		if (isEnable()) {
			Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
			if (pairedDevices != null) {
				for (BluetoothDevice bluetoothDevice : pairedDevices) {
					if ("Bluetooth Mouse".equals(bluetoothDevice.getName())
							&& bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
