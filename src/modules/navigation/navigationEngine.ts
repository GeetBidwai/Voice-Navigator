import { GeoPoint, ManeuverInstruction, NavigationSession } from '../../types/navigation';
import { OpenRouteService } from '../routing/openRouteService';
import { SpeechOutputService } from '../voice/speechOutput';

export class NavigationEngineService {
  private currentSession: NavigationSession = {
    isActive: false,
    destinationName: '',
    maneuvers: [],
    currentStepIndex: 0,
    totalDistanceMeters: 0,
    totalDurationSeconds: 0,
  };

  private lastAnnouncement = 'Where would you like to go?';

  constructor(
    private ors: OpenRouteService,
    private speech: SpeechOutputService
  ) {}

  getSession(): NavigationSession {
    return this.currentSession;
  }

  async startRoute(
    destination: string,
    currentLocation: GeoPoint | null
  ): Promise<{ announcement: string; session: NavigationSession }> {
    if (!currentLocation) {
      const err = 'GPS location unavailable. Please enable device location.';
      await this.speech.speak(err);
      return { announcement: err, session: this.currentSession };
    }

    try {
      await this.speech.speak(`Finding route to ${destination}`);
      const destCoords = await this.ors.geocode(destination, currentLocation);
      const session = await this.ors.getRoute(currentLocation, destCoords);

      this.currentSession = session;
      const firstManeuver = session.maneuvers[0]?.instruction || 'Route started.';
      const announcement = `Navigating to ${destCoords.label || destination}. ${firstManeuver}`;

      this.lastAnnouncement = announcement;
      await this.speech.speak(announcement);
      return { announcement, session };
    } catch (err: any) {
      const msg = err.message || 'Routing error occurred.';
      await this.speech.speak(msg);
      return { announcement: msg, session: this.currentSession };
    }
  }

  howFar(): string {
    if (!this.currentSession.isActive) {
      const msg = 'No active navigation route.';
      this.speech.speak(msg);
      return msg;
    }
    const km = (this.currentSession.totalDistanceMeters / 1000).toFixed(1);
    const mins = Math.round(this.currentSession.totalDurationSeconds / 60);
    const text = `${km} kilometers remaining, approximately ${mins} minutes.`;
    this.speech.speak(text);
    return text;
  }

  nextTurn(): string {
    if (!this.currentSession.isActive) {
      const msg = 'No active navigation route.';
      this.speech.speak(msg);
      return msg;
    }
    const current = this.currentSession.maneuvers[this.currentSession.currentStepIndex];
    const text = current
      ? `In ${current.distanceMeters} meters, ${current.instruction}`
      : 'Continue straight on current road.';
    this.lastAnnouncement = text;
    this.speech.speak(text);
    return text;
  }

  repeat(): string {
    this.speech.speak(this.lastAnnouncement);
    return this.lastAnnouncement;
  }

  stop(): string {
    this.currentSession = {
      isActive: false,
      destinationName: '',
      maneuvers: [],
      currentStepIndex: 0,
      totalDistanceMeters: 0,
      totalDurationSeconds: 0,
    };
    const text = 'Navigation stopped.';
    this.lastAnnouncement = 'Where would you like to go?';
    this.speech.speak(text);
    return text;
  }
}
