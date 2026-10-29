import * as Location from 'expo-location';
import { GeoPoint } from '../../types/navigation';

export class LocationTrackerService {
  private subscription: Location.LocationSubscription | null = null;

  async requestPermission(): Promise<boolean> {
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      return status === 'granted';
    } catch {
      return false;
    }
  }

  async getCurrentLocation(): Promise<GeoPoint | null> {
    const hasPerm = await this.requestPermission();
    if (!hasPerm) return null;

    try {
      const loc = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.High,
      });
      return {
        latitude: loc.coords.latitude,
        longitude: loc.coords.longitude,
        label: 'Current GPS Location',
      };
    } catch (err) {
      console.warn('Failed to obtain GPS position', err);
      return null;
    }
  }

  async startTracking(onLocationUpdate: (point: GeoPoint) => void): Promise<void> {
    const hasPerm = await this.requestPermission();
    if (!hasPerm) return;

    try {
      this.subscription = await Location.watchPositionAsync(
        {
          accuracy: Location.Accuracy.High,
          timeInterval: 4000,
          distanceInterval: 10,
        },
        (loc) => {
          onLocationUpdate({
            latitude: loc.coords.latitude,
            longitude: loc.coords.longitude,
            label: 'Current GPS Location',
          });
        }
      );
    } catch (err) {
      console.warn('Error starting location watcher', err);
    }
  }

  stopTracking() {
    if (this.subscription) {
      this.subscription.remove();
      this.subscription = null;
    }
  }
}
