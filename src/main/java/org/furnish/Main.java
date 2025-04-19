package org.furnish;

import javax.swing.SwingUtilities;

import org.furnish.utils.FirebaseUtil;

public class Main {
    public static void main(String[] args) {
        FirebaseUtil.initializeFirebase();
        SwingUtilities.invokeLater(() -> new OnboardingScreen().setVisible(true));
    }
}