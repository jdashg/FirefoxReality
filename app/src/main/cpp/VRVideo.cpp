/* -*- Mode: C++; tab-width: 20; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "VRVideo.h"
#include "vrb/ConcreteClass.h"
#include "vrb/Color.h"
#include "vrb/CreationContext.h"
#include "vrb/Geometry.h"
#include "vrb/Matrix.h"
#include "vrb/ModelLoaderAndroid.h"
#include "vrb/RenderState.h"
#include "vrb/RenderContext.h"
#include "vrb/TextureGL.h"
#include "vrb/TextureSurface.h"
#include "vrb/Toggle.h"
#include "vrb/Transform.h"
#include "vrb/VertexArray.h"

#include "Quad.h"
#include "Widget.h"

namespace crow {

// Should match the values in VideoProjectionMenuWidget.java
enum class VRVideoProjection {
  VIDEO_PROJECTION_3D_SIDE_BY_SIDE = 0,
  VIDEO_PROJECTION_360 = 1,
  VIDEO_PROJECTION_360_STEREO = 2,
  VIDEO_PROJECTION_180 = 3,
  VIDEO_PROJECTION_180_STEREO_LEFT_RIGTH = 4,
  VVIDEO_PROJECTION_180_STEREO_TOP_BOTTOM = 5,
};

struct VRVideo::State {
  vrb::CreationContextWeak context;
  WidgetPtr window;
  VRVideoProjection projection;
  vrb::TogglePtr root;
  vrb::TogglePtr leftEye;
  vrb::TogglePtr rightEye;
  State()
  {
  }

  void Initialize(const WidgetPtr& aWindow, const int aProjection) {
    vrb::CreationContextPtr create = context.lock();
    window = aWindow;
    projection = static_cast<VRVideoProjection>(aProjection);
    root = vrb::Toggle::Create(create);
    updateProjection();
    root->AddNode(leftEye);
    if (rightEye) {
      root->AddNode(rightEye);
    }
  }

  void updateProjection() {
    switch (projection) {
      case VRVideoProjection::VIDEO_PROJECTION_3D_SIDE_BY_SIDE:
        break;
      case VRVideoProjection ::VIDEO_PROJECTION_360:
        leftEye = create360Projection(device::EyeRect(0.0f, 0.0f, 1.0f, 1.0f));
        break;
      case VRVideoProjection ::VIDEO_PROJECTION_360_STEREO:
        leftEye = create360Projection(device::EyeRect(0.0f, 0.0f, 0.5f, 0.5f));
        rightEye = create360Projection(device::EyeRect(0.5f, 0.5f, 0.5f, 0.5f));
        break;
      default:
        break;
    }
  }

  vrb::TogglePtr create360Projection(device::EyeRect aUVRect) {
    const int kRows = 60;
    const int kCols = 60;
    const float kRadius = 10.0f;

    vrb::CreationContextPtr create = context.lock();
    vrb::VertexArrayPtr array = vrb::VertexArray::Create(create);

    const float alphaDelta = 2.0f * (float)M_PI / kCols;
    const float betaDelta = (float)M_PI / kRows;
    for (int row = 0; row < kRows; ++row) {
      const float beta = betaDelta * row;
      vrb::Vector vertex;
      vrb::Vector uv;
      vertex.y() = kRadius * cosf(beta);
      uv.y() = aUVRect.mY + aUVRect.mHeight * (float)row / (float)kRows;
      for (int col = 0; col < kCols; ++col) {
        const float alpha = alphaDelta * col;
        vertex.x() = kRadius * sinf(beta) * cosf(alpha);
        vertex.z() = kRadius * sinf(beta) * sinf(alpha);
        uv.x() = aUVRect.mX + aUVRect.mWidth * (float)col / (float)kCols;

        array->AppendVertex(vertex);
        array->AppendUV(uv);
        array->AppendNormal(vertex.Normalize());
      }
    }

    vrb::RenderStatePtr state = vrb::RenderState::Create(create);
    state->SetLightsEnabled(false);
    vrb::TexturePtr texture = std::dynamic_pointer_cast<vrb::Texture>(window->GetSurfaceTexture());
    state->SetTexture(texture);
    vrb::GeometryPtr geometry = vrb::Geometry::Create(create);
    geometry->SetVertexArray(array);
    geometry->SetRenderState(state);


    // indices
    std::vector<int> index;
    for (int row = 0; row < kRows; ++row) {
      for (int col = 0; col < kCols; ++col) {
        const int a = row * kRows + col + 1;
        const int b = row * kRows + col;
        const int c = (row + 1) * kRows + col;
        const int d = (row + 1) * kRows + col + 1;

        if (row > 0) {
          index = {a, b, d};
          geometry->AddFace(index, index, index);
        }
        if (row != row - 1) {
          index = {b, c, d};
          geometry->AddFace(index, index, index);
        }
      }
    }

    vrb::TransformPtr transform = vrb::Transform::Create(create);
    transform->SetTransform(vrb::Matrix::Position(vrb::Vector(0.0f, 0.0f, 0.0f)));
    transform->AddNode(geometry);

    vrb::TogglePtr result = vrb::Toggle::Create(create);
    result->AddNode(transform);
    return result;
  }


};

void
VRVideo::SelectEye(device::Eye aEye) {
  if (!m.rightEye) {
    // Not stereo projection, always show the left eye.
    return;
  }
  m.leftEye->ToggleAll(aEye == device::Eye::Left);
  m.rightEye->ToggleAll(aEye == device::Eye::Right);
}

vrb::NodePtr
VRVideo::GetRoot() const {
  return m.root;
}

VRVideoPtr
VRVideo::Create(vrb::CreationContextPtr aContext, const WidgetPtr& aWindow, const int aProjection) {
  VRVideoPtr result = std::make_shared<vrb::ConcreteClass<VRVideo, VRVideo::State> >(aContext);
  result->m.Initialize(aWindow, aProjection);
  return result;
}


VRVideo::VRVideo(State& aState, vrb::CreationContextPtr& aContext) : m(aState) {
  m.context = aContext;
}

VRVideo::~VRVideo() {}

} // namespace crow
