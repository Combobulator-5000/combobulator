package com.enph.plab.java.combobulator

// Using this instead of an enum since enums can't be const
const val JSON = 0
const val REALM = 1


const val DATASOURCE = JSON
const val WORKSPACE_FILE = "workspaces/project_fair.json"


// Navigation
const val THRESHOLD_DISTANCE = 0.2

// Classifier
const val CLASSIFIER_DISTANCE_RATIO = 0.7
const val FLANN_MATCHER_PARAMS = "matcher_params.yaml"

// UI
const val DEBUG_ON_AT_STARTUP = false