import React from 'react';
import PropTypes from 'prop-types';
import {
  View,
  StyleSheet,
  NativeModules,
  requireNativeComponent,
} from 'react-native';

import { isAndroid, isFunction, viewPropTypes } from '../utils';

import NativeBridgeComponent from './NativeBridgeComponent';

const MapboxGL = NativeModules.MGLModule;
if (MapboxGL == null) {
  console.error(
    'Native part of Mapbox React Native libraries were not registered properly, double check our native installation guides.',
  );
}

export const NATIVE_MODULE_NAME = 'RCTMGLMapNavigationView';

const styles = StyleSheet.create({
  matchParent: { flex: 1 },
});

/**
 * MapNavigationView backed by Mapbox Native GL
 */
class MapNavigationView extends NativeBridgeComponent(
  React.Component,
  NATIVE_MODULE_NAME,
) {
  static propTypes = {
    ...viewPropTypes,

    fromLatitude: PropTypes.number,
    fromLongitude: PropTypes.number,
    toLatitude: PropTypes.number,
    toLongitude: PropTypes.number,

    onShowResumeButton: PropTypes.func,
    onDidArrive: PropTypes.func,
    onUpdateNavigationInfo: PropTypes.func,
  };

  static defaultProps = {};

  constructor(props) {
    super(props);

    this.state = {
      isReady: null,
      region: null,
      width: 0,
      height: 0,
      isUserInteraction: false,
    };

    this._onShowResumeButton = this._onShowResumeButton.bind(this);
    this._onDidArrive = this._onDidArrive.bind(this);
    this._onUpdateNavigationInfo = this._onUpdateNavigationInfo.bind(this);
    this._onLayout = this._onLayout.bind(this);
  }

  componentDidMount() {}

  componentWillUnmount() {}

  async setVoiceMuted(voiceMuted = false) {
    const res = await this._runNativeCommand('setVoiceMuted', this._nativeRef, [
      voiceMuted,
    ]);
  }

  async isVoiceMuted() {
    const res = await this._runNativeCommand('isVoiceMuted', this._nativeRef);
    return res.isVoiceMuted;
  }

  async recenter() {
    const res = await this._runNativeCommand('recenter', this._nativeRef);
  }

  _onShowResumeButton(e) {
    if (isFunction(this.props.onShowResumeButton)) {
      this.props.onShowResumeButton(e.nativeEvent.payload);
    }
  }

  _onDidArrive(e) {
    if (isFunction(this.props.onDidArrive)) {
      this.props.onDidArrive(e.nativeEvent.payload);
    }
  }

  _onUpdateNavigationInfo(e) {
    if (isFunction(this.props.onUpdateNavigationInfo)) {
      this.props.onUpdateNavigationInfo(e.nativeEvent.payload);
    }
  }

  _onLayout(e) {
    this.setState({
      isReady: true,
      width: e.nativeEvent.layout.width,
      height: e.nativeEvent.layout.height,
    });
  }

  _getContentInset() {
    if (!this.props.contentInset) {
      return;
    }

    if (!Array.isArray(this.props.contentInset)) {
      return [this.props.contentInset];
    }

    return this.props.contentInset;
  }

  _setNativeRef(nativeRef) {
    this._nativeRef = nativeRef;
    super._runPendingNativeCommands(nativeRef);
  }

  setNativeProps(props) {
    if (this._nativeRef) {
      this._nativeRef.setNativeProps(props);
    }
  }

  render() {
    const props = {
      ...this.props,
      contentInset: this._getContentInset(),
      style: styles.matchParent,
    };

    const callbacks = {
      ref: (nativeRef) => this._setNativeRef(nativeRef),
      onShowResumeButton: this._onShowResumeButton,
      onDidArrive: this._onDidArrive,
      onUpdateNavigationInfo: this._onUpdateNavigationInfo,
      onAndroidCallback: isAndroid() ? this._onAndroidCallback : undefined,
    };

    let mapView = (
      <RCTMGLMapNavigationView {...props} {...callbacks}>
        {this.props.children}
      </RCTMGLMapNavigationView>
    );

    return (
      <View
        onLayout={this._onLayout}
        style={this.props.style}
        testID={mapView ? null : this.props.testID}
      >
        {mapView}
      </View>
    );
  }
}

const RCTMGLMapNavigationView = requireNativeComponent(
  NATIVE_MODULE_NAME,
  MapNavigationView,
  {
    nativeOnly: { onAndroidCallback: true },
  },
);

export default MapNavigationView;
