import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { COLORS } from '../theme/colors';

interface MicrophoneButtonProps {
  isListening: boolean;
  onPress: () => void;
}

export const MicrophoneButton: React.FC<MicrophoneButtonProps> = ({ isListening, onPress }) => {
  return (
    <View style={styles.outerRing}>
      <TouchableOpacity
        activeOpacity={0.85}
        onPress={onPress}
        style={[
          styles.button,
          {
            backgroundColor: isListening ? COLORS.amberAccent : COLORS.surfaceVariant,
          },
        ]}
        accessibilityRole="button"
        accessibilityLabel={isListening ? 'Listening' : 'Ask'}
      >
        <Text
          style={[
            styles.micGlyph,
            { color: isListening ? COLORS.background : COLORS.text },
          ]}
        >
          🎙
        </Text>
        <Text
          style={[
            styles.label,
            { color: isListening ? COLORS.background : COLORS.text },
          ]}
        >
          {isListening ? 'Listening' : 'Ask'}
        </Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  outerRing: {
    width: 140,
    height: 140,
    borderRadius: 70,
    borderWidth: 3,
    borderColor: COLORS.surfaceVariant,
    justifyContent: 'center',
    alignItems: 'center',
  },
  button: {
    width: 120,
    height: 120,
    borderRadius: 60,
    justifyContent: 'center',
    alignItems: 'center',
  },
  micGlyph: {
    fontSize: 32,
    marginBottom: 4,
  },
  label: {
    fontSize: 16,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
});
