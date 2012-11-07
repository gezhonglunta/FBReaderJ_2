package org.geometerplus.android.fbreader;

import java.util.Set;

import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

public class BluetoothDeviceHelper {
	private static BluetoothDeviceHelper instance;
	private boolean mhasBluetoothMouse;

	public static BluetoothDeviceHelper Instance() {
		if (instance == null) {
			instance = new BluetoothDeviceHelper();
		}
		return instance;
	}

	public BluetoothDeviceHelper() {
		FBReaderApp.Instance()
				.addTimerTask(new BluetoothMouseCheckTask(), 5000);
	}

	public boolean hasBluetoothMouse() {
		return mhasBluetoothMouse;
	}

	public void MouseLeftClick() {
		if (FBReaderApp.Instance().isAutoBrowsing()) {
			FBReaderApp.Instance().runAction(ActionCode.AUTO_BROWSE_OFF,
					new Object[] {});
		} else {
			FBReaderApp.Instance().runAction(ActionCode.AUTO_BROWSE_ON,
					new Object[] {});
		}
	}

	public void MouseRightClick() {

	}

	public void Wheel() {

	}

	private void setHasBluetoothMouse(boolean b) {
		mhasBluetoothMouse = b;
	}

	private class BluetoothMouseCheckTask implements Runnable {
		BluetoothAdapter adapter;

		public BluetoothMouseCheckTask() {
			try {
				adapter = BluetoothAdapter.getDefaultAdapter();
			} catch (Exception e) {
			}
		}

		public void run() {
			try {
				if (adapter != null && adapter.isEnabled()) {
					Set<BluetoothDevice> pairedDevices = adapter
							.getBondedDevices();
					if (pairedDevices != null) {
						for (BluetoothDevice bluetoothDevice : pairedDevices) {
							if ("Bluetooth Mouse".equals(bluetoothDevice
									.getName())
									&& bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
								BluetoothDeviceHelper.Instance()
										.setHasBluetoothMouse(true);
								return;
							}
						}
					}
				}
			} catch (Exception e) {

			}
			BluetoothDeviceHelper.Instance().setHasBluetoothMouse(false);
		}
	}
}
