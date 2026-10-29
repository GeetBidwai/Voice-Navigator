export type NavCommandType =
  | { type: 'NAVIGATE_TO'; destination: string }
  | { type: 'FIND_NEAREST'; poiType: string }
  | { type: 'HOW_FAR' }
  | { type: 'NEXT_TURN' }
  | { type: 'REPEAT' }
  | { type: 'REROUTE' }
  | { type: 'STOP_NAVIGATION' }
  | { type: 'HELP' }
  | { type: 'UNKNOWN'; rawText: string };

export interface ManeuverInstruction {
  instruction: string;
  distanceMeters: number;
  streetName: string;
  turnType: string;
}

export interface GeoPoint {
  latitude: number;
  longitude: number;
  label?: string;
}

export interface NavigationSession {
  isActive: boolean;
  destinationName: string;
  destinationCoords?: GeoPoint;
  maneuvers: ManeuverInstruction[];
  currentStepIndex: number;
  totalDistanceMeters: number;
  totalDurationSeconds: number;
}

export interface VoiceNavState {
  instructionText: string;
  youSaidText: string;
  isListening: boolean;
  isSimulatedListening: boolean;
  isSettingsOpen: boolean;
  apiKey: string;
  isApiKeyConfigured: boolean;
  isGpsEnabled: boolean;
  currentLocation: GeoPoint | null;
  isRouting: boolean;
  session: NavigationSession;
}
