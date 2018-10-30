/* -*- Mode: C++; tab-width: 20; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef VRBROWSER_VR_VIDEO_H
#define VRBROWSER_VR_VIDEO_H

#include "vrb/Forward.h"
#include "vrb/MacroUtils.h"
#include "Device.h"

#include <memory>
#include <string>
#include <vector>
#include <functional>

namespace crow {

class VRVideo;
typedef std::shared_ptr<VRVideo> VRVideoPtr;

class Widget;
typedef std::shared_ptr<Widget> WidgetPtr;

class VRVideo {
public:
  static VRVideoPtr Create(vrb::CreationContextPtr aContext, const WidgetPtr& aWindow, const int aProjection);
  void SelectEye(device::Eye aEye);
  vrb::NodePtr GetRoot() const;

  struct State;
  VRVideo(State& aState, vrb::CreationContextPtr& aContext);
  ~VRVideo();
private:
  State& m;
  VRVideo() = delete;
  VRB_NO_DEFAULTS(VRVideo)
};

} // namespace crow

#endif // VRBROWSER_VR_VIDEO_H
