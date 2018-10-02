/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.meshprovisioner;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigAppKeyAddState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigAppKeyStatusState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigCompositionDataGetState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigCompositionDataStatusState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigMessageState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigModelAppBindState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigModelAppStatusState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigModelAppUnbindState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigModelPublicationSetState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigModelPublicationStatusState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigModelSubscriptionAddState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigModelSubscriptionDeleteState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigModelSubscriptionStatusState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigNodeResetState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ConfigNodeResetStatusState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.DefaultNoOperationMessageState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.GenericLevelGetState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.GenericLevelSetState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.GenericLevelSetUnacknowledgedState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.GenericLevelStatusState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.GenericMessageState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.GenericOnOffGetState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.GenericOnOffSetState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.GenericOnOffSetUnacknowledgedState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.GenericOnOffStatusState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.MeshMessageState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.MeshModel;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.ProvisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.VendorModelMessageAckedState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.VendorModelMessageState;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.VendorModelMessageStateStatus;
import no.nordicsemi.android.meshprovisioner.meshmessagestates.VendorModelMessageUnackedState;
import no.nordicsemi.android.meshprovisioner.messages.ConfigAppKeyAdd;
import no.nordicsemi.android.meshprovisioner.messages.ConfigCompositionDataGet;
import no.nordicsemi.android.meshprovisioner.messages.ConfigModelAppBind;
import no.nordicsemi.android.meshprovisioner.messages.ConfigModelAppUnbind;
import no.nordicsemi.android.meshprovisioner.messages.ConfigModelPublicationSet;
import no.nordicsemi.android.meshprovisioner.messages.ConfigModelSubscriptionAdd;
import no.nordicsemi.android.meshprovisioner.messages.ConfigModelSubscriptionDelete;
import no.nordicsemi.android.meshprovisioner.messages.ConfigNodeReset;
import no.nordicsemi.android.meshprovisioner.messages.GenericLevelGet;
import no.nordicsemi.android.meshprovisioner.messages.GenericLevelSet;
import no.nordicsemi.android.meshprovisioner.messages.GenericLevelSetUnacknowledged;
import no.nordicsemi.android.meshprovisioner.messages.GenericOnOffGet;
import no.nordicsemi.android.meshprovisioner.messages.GenericOnOffSet;
import no.nordicsemi.android.meshprovisioner.messages.GenericOnOffSetUnacknowledged;
import no.nordicsemi.android.meshprovisioner.messages.MeshMessage;
import no.nordicsemi.android.meshprovisioner.messages.VendorModelMessageAcked;
import no.nordicsemi.android.meshprovisioner.messages.VendorModelMessageUnacked;
import no.nordicsemi.android.meshprovisioner.models.VendorModel;
import no.nordicsemi.android.meshprovisioner.opcodes.ConfigMessageOpCodes;
import no.nordicsemi.android.meshprovisioner.utils.ConfigModelPublicationSetParams;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

class MeshMessageHandler implements MeshMessageHandlerApi, InternalMeshMsgHandlerCallbacks {

    private static final String TAG = MeshMessageHandler.class.getSimpleName();

    private final Context mContext;
    private final InternalTransportCallbacks mInternalTransportCallbacks;
    private final InternalMeshManagerCallbacks mInternalMeshManagerCallbacks;
    private MeshStatusCallbacks mStatusCallbacks;
    private MeshMessageState mMeshMessageState;

    MeshMessageHandler(final Context context, final InternalTransportCallbacks internalTransportCallbacks, final InternalMeshManagerCallbacks internalMeshManagerCallbacks) {
        this.mContext = context;
        this.mInternalTransportCallbacks = internalTransportCallbacks;
        this.mInternalMeshManagerCallbacks = internalMeshManagerCallbacks;
    }

    void setMeshStatusCallbacks(final MeshStatusCallbacks statusCallbacks) {
        this.mStatusCallbacks = statusCallbacks;
    }

    /**
     * Handle mesh message states on write callback complete
     * <p>
     * This method will jump to the current state and switch the current state according to the message that has been sent.
     * </p>
     *
     * @param meshNode Corresponding mesh node
     * @param pdu      mesh pdu that was sent
     */
    void handleMeshMsgWriteCallbacks(final ProvisionedMeshNode meshNode, final byte[] pdu) {
        if (mMeshMessageState instanceof ConfigMessageState) {
            if (mMeshMessageState.getState() == null)
                return;

            switch (mMeshMessageState.getState()) {
                case COMPOSITION_DATA_GET_STATE:
                    //Composition data get complete,
                    //We directly switch to next state because there is no acknowledgements involved
                    final ConfigCompositionDataStatusState compositionDataStatus = new ConfigCompositionDataStatusState(mContext, meshNode, this);
                    compositionDataStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    compositionDataStatus.setStatusCallbacks(mStatusCallbacks);
                    switchState(compositionDataStatus);
                    break;
                case APP_KEY_ADD_STATE:
                    final ConfigAppKeyAddState configAppKeyAdd = ((ConfigAppKeyAddState) mMeshMessageState);
                    //Create the next corresponding status state
                    final ConfigAppKeyStatusState configAppKeyStatus = new ConfigAppKeyStatusState(mContext, meshNode, configAppKeyAdd.getSrc(),
                            configAppKeyAdd.getAppKey(), this);
                    configAppKeyStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    configAppKeyStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(configAppKeyStatus);
                    break;
                case CONFIG_MODEL_APP_BIND_STATE:
                    final ConfigModelAppBindState configModelAppBind = ((ConfigModelAppBindState) mMeshMessageState);
                    //Create the next corresponding status state
                    final ConfigModelAppStatusState configModelAppBindStatus = new ConfigModelAppStatusState(mContext, meshNode, configModelAppBind.getState().getState(), this);
                    configModelAppBindStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    configModelAppBindStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(configModelAppBindStatus);
                    break;
                case CONFIG_MODEL_APP_UNBIND_STATE:
                    final ConfigModelAppUnbindState configModelAppUnbind = ((ConfigModelAppUnbindState) mMeshMessageState);
                    //Create the next corresponding status state
                    final ConfigModelAppStatusState configModelAppUnbindStatus = new ConfigModelAppStatusState(mContext, meshNode, configModelAppUnbind.getState().getState(), this);
                    configModelAppUnbindStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    configModelAppUnbindStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(configModelAppUnbindStatus);
                    break;
                case CONFIG_MODEL_PUBLICATION_SET_STATE:
                    // Create the corresponding status state
                    final ConfigModelPublicationStatusState configModelPublicationStatus = new ConfigModelPublicationStatusState(mContext, meshNode, this);
                    configModelPublicationStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    configModelPublicationStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(configModelPublicationStatus);
                    break;
                case CONFIG_MODEL_SUBSCRIPTION_ADD_STATE:
                    // Create the corresponding status state
                    final ConfigModelSubscriptionStatusState subscriptionAddStatus = new ConfigModelSubscriptionStatusState(mContext, meshNode,
                            ConfigMessageOpCodes.CONFIG_MODEL_SUBSCRIPTION_ADD, this);
                    subscriptionAddStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    subscriptionAddStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(subscriptionAddStatus);
                    break;
                case CONFIG_MODEL_SUBSCRIPTION_DELETE_STATE:
                    //Create the next corresponding status state
                    final ConfigModelSubscriptionStatusState subscriptionDeleteStatus = new ConfigModelSubscriptionStatusState(mContext, meshNode,
                            ConfigMessageOpCodes.CONFIG_MODEL_SUBSCRIPTION_DELETE, this);
                    subscriptionDeleteStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    subscriptionDeleteStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(subscriptionDeleteStatus);
                    break;
                case CONFIG_NODE_RESET_STATE:
                    //Create the next corresponding status state
                    final ConfigNodeResetStatusState configNodeResetStatus = new ConfigNodeResetStatusState(mContext, mMeshMessageState.getMeshNode(), this);
                    configNodeResetStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    configNodeResetStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(configNodeResetStatus);
                    break;
            }
        } else if (mMeshMessageState instanceof GenericMessageState) {
            if (mMeshMessageState.getState() == null)
                return;

            switch (mMeshMessageState.getState()) {
                case GENERIC_ON_OFF_GET_STATE:
                    //Create the next corresponding status state
                    final GenericOnOffStatusState genericOnOffGetStatus = new GenericOnOffStatusState(mContext, mMeshMessageState.getMeshNode(), this, mMeshMessageState.getMeshModel(),
                            mMeshMessageState.getAppKeyIndex());
                    genericOnOffGetStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    genericOnOffGetStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(genericOnOffGetStatus);
                    break;
                case GENERIC_ON_OFF_SET_STATE:
                    //Create the next corresponding status state
                    final GenericOnOffStatusState genericOnOffSetStatus = new GenericOnOffStatusState(mContext, mMeshMessageState.getMeshNode(), this, mMeshMessageState.getMeshModel(),
                            mMeshMessageState.getAppKeyIndex());
                    genericOnOffSetStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    genericOnOffSetStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(genericOnOffSetStatus);
                    break;
                case GENERIC_ON_OFF_SET_UNACKNOWLEDGED_STATE:
                    //We don't expect a generic on off status as this is an unacknowledged message so we switch states here
                    switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
                    break;
                case GENERIC_LEVEL_GET_STATE:
                    //Create the next corresponding status state
                    final GenericLevelStatusState genericLevelGetStatus = new GenericLevelStatusState(mContext, mMeshMessageState.getMeshNode(), this, mMeshMessageState.getMeshModel(),
                            mMeshMessageState.getAppKeyIndex());
                    genericLevelGetStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    genericLevelGetStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(genericLevelGetStatus);
                    break;
                case GENERIC_LEVEL_SET_STATE:
                    //Create the next corresponding status state
                    final GenericLevelStatusState genericLevelSetStatus = new GenericLevelStatusState(mContext, mMeshMessageState.getMeshNode(), this, mMeshMessageState.getMeshModel(),
                            mMeshMessageState.getAppKeyIndex());
                    genericLevelSetStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    genericLevelSetStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(genericLevelSetStatus);
                    break;
                case GENERIC_LEVEL_SET_UNACKNOWLEDGED_STATE:
                    //We don't expect a generic on off status as this is an unacknowledged message so we switch states here
                    switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
                    break;
            }
        } else if (mMeshMessageState instanceof VendorModelMessageState) {
            if (mMeshMessageState instanceof VendorModelMessageUnackedState) {
                //We don't expect a generic on off status as this is an unacknowledged message so we switch states here
                switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
            } else {
                final VendorModelMessageStateStatus vendorModelMessage = new VendorModelMessageStateStatus(mContext, mMeshMessageState.getMeshNode(), this,
                        mMeshMessageState.getMeshModel(), mMeshMessageState.getAppKeyIndex());
                vendorModelMessage.setTransportCallbacks(mInternalTransportCallbacks);
                vendorModelMessage.setStatusCallbacks(mStatusCallbacks);
                switchState(vendorModelMessage);
            }
        }
    }

    /**
     * Handle mesh states on receiving mesh message notifications
     * <p>
     * This method will jump to the current state and switch the state depending on the expected and the next message received.
     * </p>
     *
     * @param meshNode Corresponding mesh node
     * @param pdu      mesh pdu that was sent
     */
    protected void parseMeshMsgNotifications(final ProvisionedMeshNode meshNode, final byte[] pdu) {
        if (mMeshMessageState instanceof ConfigMessageState) {
            final ConfigMessageState message = (ConfigMessageState) mMeshMessageState;
            switch (message.getState()) {
                case COMPOSITION_DATA_STATUS_STATE:
                    final ConfigCompositionDataStatusState compositionDataStatus = (ConfigCompositionDataStatusState) mMeshMessageState;
                    if (compositionDataStatus.parseMeshPdu(pdu)) {
                        //mInternalMeshManagerCallbacks.onUnicastAddressChanged(compositionDataStatus.getUnicastAddress());
                        switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
                    }
                    break;
                case APP_KEY_ADD_STATE:
                    final ConfigAppKeyAddState configAppKeyAdd = ((ConfigAppKeyAddState) mMeshMessageState);
                    //Create the next corresponding status state
                    final ConfigAppKeyStatusState configAppKeyStatus = new ConfigAppKeyStatusState(mContext, meshNode, configAppKeyAdd.getSrc(),
                            configAppKeyAdd.getAppKey(), this);
                    configAppKeyStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    configAppKeyStatus.setStatusCallbacks(mStatusCallbacks);
                    switchState(configAppKeyStatus, pdu);
                    break;
                case APP_KEY_STATUS_STATE:
                    final ConfigAppKeyStatusState status = ((ConfigAppKeyStatusState) mMeshMessageState);
                    if (status.isIncompleteTimerExpired() || (status.parseMeshPdu(pdu))) {
                        switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
                    }
                    break;
                case CONFIG_MODEL_APP_BIND_STATE:
                    final ConfigModelAppBindState configModelAppBind = ((ConfigModelAppBindState) mMeshMessageState);
                    //Create the next corresponding status state
                    final ConfigModelAppStatusState configModelAppStatus = new ConfigModelAppStatusState(mContext, meshNode, configModelAppBind.getState().getState(), this);
                    configModelAppStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    configModelAppStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(configModelAppStatus, pdu);
                    break;
                case CONFIG_MODEL_APP_UNBIND_STATE:
                    final ConfigModelAppUnbindState configModelAppUnbind = ((ConfigModelAppUnbindState) mMeshMessageState);
                    //Create the next corresponding status state
                    final ConfigModelAppStatusState configModelAppStatus1 = new ConfigModelAppStatusState(mContext, meshNode, configModelAppUnbind.getState().getState(), this);
                    configModelAppStatus1.setTransportCallbacks(mInternalTransportCallbacks);
                    configModelAppStatus1.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(configModelAppStatus1, pdu);
                    break;
                case CONFIG_MODEL_APP_STATUS_STATE:
                    if (((ConfigModelAppStatusState) mMeshMessageState).parseMeshPdu(pdu)) {
                        switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
                    }
                    break;
                case CONFIG_MODEL_PUBLICATION_SET_STATE:
                    // Create the corresponding status state
                    final ConfigModelPublicationStatusState configModelPublicationStatus = new ConfigModelPublicationStatusState(mContext, meshNode, this);
                    configModelPublicationStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    configModelPublicationStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(configModelPublicationStatus, pdu);
                    break;
                case CONFIG_MODEL_PUBLICATION_STATUS_STATE:
                    if (((ConfigModelPublicationStatusState) mMeshMessageState).parseMeshPdu(pdu)) {
                        switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
                    }
                    break;
                case CONFIG_MODEL_SUBSCRIPTION_ADD_STATE:
                    // Create the corresponding status state
                    final ConfigModelSubscriptionStatusState configModelSubscriptionStatus = new ConfigModelSubscriptionStatusState(mContext, meshNode,
                            ConfigMessageOpCodes.CONFIG_MODEL_SUBSCRIPTION_ADD, this);
                    configModelSubscriptionStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    configModelSubscriptionStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(configModelSubscriptionStatus, pdu);
                    break;
                case CONFIG_MODEL_SUBSCRIPTION_DELETE_STATE:
                    //Create the next corresponding status state
                    final ConfigModelSubscriptionStatusState configModelSubscriptionStatus1 = new ConfigModelSubscriptionStatusState(mContext, meshNode,
                            ConfigMessageOpCodes.CONFIG_MODEL_SUBSCRIPTION_DELETE, this);
                    configModelSubscriptionStatus1.setTransportCallbacks(mInternalTransportCallbacks);
                    configModelSubscriptionStatus1.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(configModelSubscriptionStatus1, pdu);
                    break;
                case CONFIG_MODEL_SUBSCRIPTION_STATUS_STATE:
                    if (((ConfigModelSubscriptionStatusState) mMeshMessageState).parseMeshPdu(pdu)) {
                        switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
                    }
                    break;
                case CONFIG_NODE_RESET_STATE:
                    //Create the next corresponding status state
                    final ConfigNodeResetStatusState configNodeResetStatus = new ConfigNodeResetStatusState(mContext, mMeshMessageState.getMeshNode(), this);
                    configNodeResetStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    configNodeResetStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(configNodeResetStatus, pdu);
                    break;
                case CONFIG_NODE_RESET_STATUS_STATE:
                    if (((ConfigNodeResetStatusState) mMeshMessageState).parseMeshPdu(pdu)) {
                        switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
                    }
                    break;
            }
        } else if (mMeshMessageState instanceof GenericMessageState) {
            final GenericMessageState message = (GenericMessageState) mMeshMessageState;
            switch (message.getState()) {
                case GENERIC_ON_OFF_GET_STATE:
                    //Create the next corresponding status state
                    final GenericOnOffStatusState genericOnOffGetStatus = new GenericOnOffStatusState(mContext, mMeshMessageState.getMeshNode(), this,
                            mMeshMessageState.getMeshModel(), message.getAppKeyIndex());
                    genericOnOffGetStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    genericOnOffGetStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(genericOnOffGetStatus, pdu);
                    break;
                case GENERIC_ON_OFF_SET_UNACKNOWLEDGED_STATE:
                    //We do nothing here since there is no status involved for unacknowledged messages
                    switchState(new DefaultNoOperationMessageState(mContext, meshNode, this), null);
                    break;
                case GENERIC_ON_OFF_SET_STATE:
                    //Create the next corresponding status state
                    final GenericOnOffStatusState genericOnOffSetStatus = new GenericOnOffStatusState(mContext, mMeshMessageState.getMeshNode(), this,
                            mMeshMessageState.getMeshModel(), mMeshMessageState.getAppKeyIndex());
                    genericOnOffSetStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    genericOnOffSetStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(genericOnOffSetStatus, pdu);
                    break;
                case GENERIC_ON_OFF_STATUS_STATE:
                    if (((GenericOnOffStatusState) mMeshMessageState).parseMeshPdu(pdu)) {
                        switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
                    }
                    break;
                case GENERIC_LEVEL_GET_STATE:
                    //Create the next corresponding status state
                    final GenericLevelStatusState genericLevelGetStatus = new GenericLevelStatusState(mContext, mMeshMessageState.getMeshNode(), this,
                            mMeshMessageState.getMeshModel(), message.getAppKeyIndex());
                    genericLevelGetStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    genericLevelGetStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(genericLevelGetStatus, pdu);
                    break;
                case GENERIC_LEVEL_SET_UNACKNOWLEDGED_STATE:
                    //We do nothing here since there is no status involved for unacknowledged messages
                    switchState(new DefaultNoOperationMessageState(mContext, meshNode, this), null);
                    break;
                case GENERIC_LEVEL_SET_STATE:
                    //Create the next corresponding status state
                    final GenericLevelStatusState genericLevelSetStatus = new GenericLevelStatusState(mContext, mMeshMessageState.getMeshNode(), this,
                            mMeshMessageState.getMeshModel(), mMeshMessageState.getAppKeyIndex());
                    genericLevelSetStatus.setTransportCallbacks(mInternalTransportCallbacks);
                    genericLevelSetStatus.setStatusCallbacks(mStatusCallbacks);
                    //Switch states
                    switchState(genericLevelSetStatus, pdu);
                    break;
                case GENERIC_LEVEL_STATUS_STATE:
                    if (((GenericLevelStatusState) mMeshMessageState).parseMeshPdu(pdu)) {
                        switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
                    }
                    break;
            }
        } else if (mMeshMessageState instanceof VendorModelMessageState) {
            if (mMeshMessageState instanceof VendorModelMessageAckedState) {
                final VendorModelMessageStateStatus vendorModelMessageStatus = new VendorModelMessageStateStatus(mContext, meshNode, this, mMeshMessageState.getMeshModel(),
                        mMeshMessageState.getAppKeyIndex());
                vendorModelMessageStatus.setTransportCallbacks(mInternalTransportCallbacks);
                vendorModelMessageStatus.setStatusCallbacks(mStatusCallbacks);
                switchState(vendorModelMessageStatus, pdu);
            } else if (mMeshMessageState instanceof VendorModelMessageStateStatus) {
                final VendorModelMessageStateStatus vendorModelMessageStatus = (VendorModelMessageStateStatus) mMeshMessageState;
                if (vendorModelMessageStatus.parseMeshPdu(pdu)) {
                    switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
                }
            } else {
                if (((VendorModelMessageUnackedState) mMeshMessageState).parseMeshPdu(pdu)) {
                    switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
                }
            }
        } else {
            ((DefaultNoOperationMessageState) mMeshMessageState).parseMeshPdu(pdu);
        }
    }

    @Override
    public void onIncompleteTimerExpired(final ProvisionedMeshNode meshNode, final byte[] src, final boolean incompleteTimerExpired) {
        //We switch no operation state if the incomplete timer has expired so that we don't wait on the same state if a particular message fails.
        switchToNoOperationState(new DefaultNoOperationMessageState(mContext, meshNode, this));
    }

    /**
     * Switch the current state of the mesh configuration handler
     * <p>
     * This method will switch the current state to the corresponding next state if the message sent was not a segmented message.
     * </p>
     *
     * @param newState new state that is to be switched to
     * @return true if the state was switched successfully
     */
    private boolean switchState(final MeshMessageState newState) {
        if (!mMeshMessageState.isSegmented()) {
            Log.v(TAG, "Switching current state on write complete " + mMeshMessageState.getClass().getSimpleName() + " to " + newState.getClass().getSimpleName());
            mMeshMessageState = newState;
            return true;
        }
        return false;
    }

    /**
     * Switch the current state of the mesh configuration handler
     * <p>
     * This method will switch the current state of the configuration handler based on the corresponding block acknowledgement pdu received.
     * The block acknowledgement pdu explains if certain segments were lost on flight, based on this we retransmit the segments that were lost on flight.
     * If there were no segments lost and the message that was sent was an acknowledged message, we switch the state to the corresponding message state.
     * </p>
     *
     * @param newState new state that is to be switched to
     * @param pdu      pdu received
     * @return true if the state was switched successfully
     */
    private boolean switchState(final MeshMessageState newState, final byte[] pdu) {
        if (pdu != null) {
            if (mMeshMessageState.isSegmented() && mMeshMessageState.isRetransmissionRequired(pdu)) {
                mMeshMessageState.executeResend();
                return false;
            } else {
                Log.v(TAG, "Switching current state " + mMeshMessageState.getClass().getSimpleName() + " to " + newState.getClass().getSimpleName());
                mMeshMessageState = newState;
                return true;
            }
        }
        return false;
    }

    private void switchToNoOperationState(final MeshMessageState newState) {
        //Switching to unknown message state here for messages that are not
        if (mMeshMessageState.getState() != null && newState.getState() != null) {
            Log.v(TAG, "Switching current state " + mMeshMessageState.getState().name() + " to No operation state");
        } else {
            Log.v(TAG, "Switched to No operation state");
        }
        newState.setTransportCallbacks(mInternalTransportCallbacks);
        newState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = newState;
    }

    ConfigMessageState.MessageState getConfigurationState() {
        return mMeshMessageState.getState();
    }

    @Override
    public final void sendCompositionDataGet(@NonNull final ProvisionedMeshNode meshNode, final int aszmic) {
        final ConfigCompositionDataGet compositionDataGet = new ConfigCompositionDataGet(meshNode, aszmic);
        final ConfigCompositionDataGetState compositionDataGetState = new ConfigCompositionDataGetState(mContext, compositionDataGet, this);
        compositionDataGetState.setTransportCallbacks(mInternalTransportCallbacks);
        compositionDataGetState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = compositionDataGetState;
        //mMeshMessageState = new ConfigCompositionDataStatus(mContext, meshNode, mInternalTransportCallbacks, mStatusCallbacks);
        compositionDataGetState.executeSend();
    }

    @Override
    public final void sendCompositionDataGet(@NonNull final ConfigCompositionDataGet compositionDataGet) {
        final ConfigCompositionDataGetState compositionDataGetState = new ConfigCompositionDataGetState(mContext, compositionDataGet, this);
        compositionDataGetState.setTransportCallbacks(mInternalTransportCallbacks);
        compositionDataGetState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = compositionDataGetState;
        //mMeshMessageState = new ConfigCompositionDataStatus(mContext, meshNode, mInternalTransportCallbacks, mStatusCallbacks);
        compositionDataGetState.executeSend();
    }

    @Override
    public final void sendAppKeyAdd(@NonNull final ProvisionedMeshNode meshNode, final int appKeyIndex, @NonNull final String appKey, final int aszmic) {
        final ConfigAppKeyAdd configAppKeyAdd = new ConfigAppKeyAdd(meshNode, MeshParserUtils.toByteArray(appKey), appKeyIndex, aszmic);
        final ConfigAppKeyAddState configAppKeyAddState = new ConfigAppKeyAddState(mContext, configAppKeyAdd, this);
        configAppKeyAddState.setTransportCallbacks(mInternalTransportCallbacks);
        configAppKeyAddState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = configAppKeyAddState;
        configAppKeyAddState.executeSend();
    }

    @Override
    public final void sendAppKeyAdd(@NonNull final ConfigAppKeyAdd configAppKeyAdd) {
        final ConfigAppKeyAddState configAppKeyAddState = new ConfigAppKeyAddState(mContext, configAppKeyAdd, this);
        configAppKeyAddState.setTransportCallbacks(mInternalTransportCallbacks);
        configAppKeyAddState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = configAppKeyAddState;
        configAppKeyAddState.executeSend();
    }

    @Override
    public final void bindAppKey(@NonNull final ProvisionedMeshNode meshNode, final int aszmic,
                                 @NonNull final byte[] elementAddress, final int modelIdentifier, final int appKeyIndex) {
        final ConfigModelAppBind configModelAppBind = new ConfigModelAppBind(meshNode, elementAddress, modelIdentifier, appKeyIndex, aszmic);
        final ConfigModelAppBindState configModelAppBindState = new ConfigModelAppBindState(mContext, configModelAppBind, this);
        configModelAppBindState.setTransportCallbacks(mInternalTransportCallbacks);
        configModelAppBindState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = configModelAppBindState;
        configModelAppBindState.executeSend();
    }

    @Override
    public final void bindAppKey(@NonNull final ConfigModelAppBind configModelAppBind) {
        final ConfigModelAppBindState configModelAppBindState = new ConfigModelAppBindState(mContext, configModelAppBind, this);
        configModelAppBindState.setTransportCallbacks(mInternalTransportCallbacks);
        configModelAppBindState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = configModelAppBindState;
        configModelAppBindState.executeSend();
    }

    @Override
    public void unbindAppKey(@NonNull final ProvisionedMeshNode meshNode, final int aszmic,
                             @NonNull final byte[] elementAddress, final int modelIdentifier, final int appKeyIndex) {
        final ConfigModelAppUnbind configModelAppUnbind = new ConfigModelAppUnbind(meshNode, elementAddress, modelIdentifier, appKeyIndex, aszmic);
        final ConfigModelAppUnbindState configModelAppBind = new ConfigModelAppUnbindState(mContext, configModelAppUnbind, this);
        configModelAppBind.setTransportCallbacks(mInternalTransportCallbacks);
        configModelAppBind.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = configModelAppBind;
        configModelAppBind.executeSend();
    }

    @Override
    public final void unbindAppKey(@NonNull final ConfigModelAppUnbind configModelAppUnbind) {
        final ConfigModelAppUnbindState configModelAppBind = new ConfigModelAppUnbindState(mContext, configModelAppUnbind, this);
        configModelAppBind.setTransportCallbacks(mInternalTransportCallbacks);
        configModelAppBind.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = configModelAppBind;
        configModelAppBind.executeSend();
    }

    @Override
    public final void sendConfigModelPublicationSet(@NonNull final ConfigModelPublicationSetParams params) {
        final ConfigModelPublicationSet configModelPublicationSet = new ConfigModelPublicationSet(params.getMeshNode(),
                params.getElementAddress(), params.getPublishAddress(), params.getAppKeyIndex(), params.getCredentialFlag(),
                params.getPublishTtl(), params.getPublicationSteps(), params.getPublicationResolution(),
                params.getPublishRetransmitCount(), params.getPublishRetransmitIntervalSteps(), params.getModelIdentifier(), params.getAszmic());

        final ConfigModelPublicationSetState configModelPublicationSetState = new ConfigModelPublicationSetState(mContext, configModelPublicationSet, this);
        configModelPublicationSetState.setTransportCallbacks(mInternalTransportCallbacks);
        configModelPublicationSetState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = configModelPublicationSetState;
        configModelPublicationSetState.executeSend();
    }

    @Override
    public final void sendConfigModelPublicationSet(@NonNull final ConfigModelPublicationSet configModelPublicationSet) {
        final ConfigModelPublicationSetState configModelPublicationSetState = new ConfigModelPublicationSetState(mContext, configModelPublicationSet, this);
        configModelPublicationSetState.setTransportCallbacks(mInternalTransportCallbacks);
        configModelPublicationSetState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = configModelPublicationSetState;
        configModelPublicationSetState.executeSend();
    }

    @Override
    public final void addSubscriptionAddress(@NonNull final ProvisionedMeshNode meshNode, final int aszmic, @NonNull final byte[] elementAddress, @NonNull final byte[] subscriptionAddress,
                                             final int modelIdentifier) {
        final ConfigModelSubscriptionAdd configModelSubscriptionAdd = new ConfigModelSubscriptionAdd(meshNode, elementAddress, subscriptionAddress, modelIdentifier, aszmic);
        final ConfigModelSubscriptionAddState configModelSubscriptionAddState = new ConfigModelSubscriptionAddState(mContext, configModelSubscriptionAdd, this);
        configModelSubscriptionAddState.setTransportCallbacks(mInternalTransportCallbacks);
        configModelSubscriptionAddState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = configModelSubscriptionAddState;
        configModelSubscriptionAddState.executeSend();
    }

    @Override
    public final void addSubscriptionAddress(@NonNull final ConfigModelSubscriptionAdd configModelSubscriptionAdd) {
        final ConfigModelSubscriptionAddState configModelSubscriptionAddState = new ConfigModelSubscriptionAddState(mContext, configModelSubscriptionAdd, this);
        configModelSubscriptionAddState.setTransportCallbacks(mInternalTransportCallbacks);
        configModelSubscriptionAddState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = configModelSubscriptionAddState;
        configModelSubscriptionAddState.executeSend();
    }

    @Override
    public final void deleteSubscriptionAddress(@NonNull final ProvisionedMeshNode meshNode, final int aszmic, @NonNull final byte[] elementAddress, @NonNull final byte[] subscriptionAddress,
                                                final int modelIdentifier) {
        final ConfigModelSubscriptionDelete configModelSubscriptionDelete = new ConfigModelSubscriptionDelete(meshNode, elementAddress, subscriptionAddress, modelIdentifier, aszmic);
        final ConfigModelSubscriptionDeleteState configModelSubscriptionDeleteState = new ConfigModelSubscriptionDeleteState(mContext, configModelSubscriptionDelete, this);
        configModelSubscriptionDeleteState.setTransportCallbacks(mInternalTransportCallbacks);
        configModelSubscriptionDeleteState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = configModelSubscriptionDeleteState;
        configModelSubscriptionDeleteState.executeSend();
    }

    @Override
    public final void deleteSubscriptionAddress(@NonNull final ConfigModelSubscriptionDelete configModelSubscriptionDelete) {
        final ConfigModelSubscriptionDeleteState configModelSubscriptionDeleteState = new ConfigModelSubscriptionDeleteState(mContext, configModelSubscriptionDelete, this);
        configModelSubscriptionDeleteState.setTransportCallbacks(mInternalTransportCallbacks);
        configModelSubscriptionDeleteState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = configModelSubscriptionDeleteState;
        configModelSubscriptionDeleteState.executeSend();
    }

    @Override
    public final void resetMeshNode(@NonNull final ProvisionedMeshNode provisionedMeshNode) {
        final ConfigNodeReset configNodeReset = new ConfigNodeReset(provisionedMeshNode, 0);
        resetMeshNode(configNodeReset);
    }

    @Override
    public void resetMeshNode(@NonNull final ConfigNodeReset configNodeReset) {
        final ConfigNodeResetState configNodeResetState = new ConfigNodeResetState(mContext, configNodeReset, this);
        configNodeResetState.setTransportCallbacks(mInternalTransportCallbacks);
        configNodeResetState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = configNodeResetState;
        configNodeResetState.executeSend();
    }

    @Override
    public final void getGenericOnOff(@NonNull final ProvisionedMeshNode node, @NonNull final MeshModel model, @NonNull final byte[] dstAddress, final boolean aszmic, final int appKeyIndex) throws IllegalArgumentException {
        if (model.getBoundAppkeys().isEmpty())
            throw new IllegalArgumentException("There are no app keys bound to this model");

        final byte[] appKey = MeshParserUtils.toByteArray(model.getBoundAppKey(appKeyIndex));
        final GenericOnOffGet genericOnOffGet = new GenericOnOffGet(node, appKey, appKeyIndex, aszmic ? 1 : 0);
        final GenericOnOffGetState genericOnOffGetState = new GenericOnOffGetState(mContext, dstAddress, genericOnOffGet, this);
        genericOnOffGetState.setTransportCallbacks(mInternalTransportCallbacks);
        genericOnOffGetState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = genericOnOffGetState;
        genericOnOffGetState.executeSend();
    }

    @Override
    public final void getGenericOnOff(@NonNull final byte[] dstAddress, @NonNull final GenericOnOffGet genericOnOffGet) {
        final GenericOnOffGetState genericOnOffGetState = new GenericOnOffGetState(mContext, dstAddress, genericOnOffGet, this);
        genericOnOffGetState.setTransportCallbacks(mInternalTransportCallbacks);
        genericOnOffGetState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = genericOnOffGetState;
        genericOnOffGetState.executeSend();
    }

    @Override
    public final void setGenericOnOff(@NonNull final ProvisionedMeshNode node, @NonNull final MeshModel model,
                                      @NonNull final byte[] dstAddress, final boolean aszmic, final int appKeyIndex,
                                      final Integer transitionSteps, final Integer transitionResolution, final Integer delay, final boolean state) throws IllegalArgumentException {
        if (model.getBoundAppkeys().isEmpty())
            throw new IllegalArgumentException("There are no app keys bound to this model");

        final byte[] appKey = MeshParserUtils.toByteArray(model.getBoundAppKey(appKeyIndex));

        final GenericOnOffSet genericOnOffSet = new GenericOnOffSet(node, appKey, appKeyIndex, state, transitionSteps, transitionResolution, delay, aszmic ? 1 : 0);
        final GenericOnOffSetState genericOnOffSetState = new GenericOnOffSetState(mContext, dstAddress, genericOnOffSet, this);
        genericOnOffSetState.setTransportCallbacks(mInternalTransportCallbacks);
        genericOnOffSetState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = genericOnOffSetState;
        genericOnOffSetState.executeSend();
    }

    @Override
    public final void setGenericOnOff(final byte[] dstAddress, final GenericOnOffSet genericOnOffSet) {
        final GenericOnOffSetState genericOnOffSetState = new GenericOnOffSetState(mContext, dstAddress, genericOnOffSet, this);
        genericOnOffSetState.setTransportCallbacks(mInternalTransportCallbacks);
        genericOnOffSetState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = genericOnOffSetState;
        genericOnOffSetState.executeSend();
    }

    @Override
    public final void setGenericOnOffUnacknowledged(@NonNull final ProvisionedMeshNode node, @NonNull final MeshModel model,
                                                    @NonNull final byte[] dstAddress, final boolean aszmic, final int appKeyIndex,
                                                    @NonNull final Integer transitionSteps, @NonNull final Integer transitionResolution,
                                                    @NonNull final Integer delay, final boolean state) throws IllegalArgumentException {
        if (model.getBoundAppkeys().isEmpty())
            throw new IllegalArgumentException("There are no app keys bound to this model");

        final byte[] appKey = MeshParserUtils.toByteArray(model.getBoundAppKey(appKeyIndex));
        final GenericOnOffSetUnacknowledged genericOnOffSetUnacked = new GenericOnOffSetUnacknowledged(node, appKey, appKeyIndex, state, transitionSteps, transitionResolution, delay, aszmic ? 1 : 0);
        setGenericOnOffUnacknowledged(dstAddress, genericOnOffSetUnacked);
    }

    @Override
    public final void setGenericOnOffUnacknowledged(@NonNull final byte[] dstAddress, @NonNull final GenericOnOffSetUnacknowledged genericOnOffSetUnacked) {
        final GenericOnOffSetUnacknowledgedState genericOnOffSetUnAckedState = new GenericOnOffSetUnacknowledgedState(mContext, dstAddress, genericOnOffSetUnacked, this);
        genericOnOffSetUnAckedState.setTransportCallbacks(mInternalTransportCallbacks);
        genericOnOffSetUnAckedState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = genericOnOffSetUnAckedState;
        genericOnOffSetUnAckedState.executeSend();
    }

    @Override
    public final void getGenericLevel(@NonNull final ProvisionedMeshNode node, @NonNull final MeshModel model, @NonNull final byte[] dstAddress, final boolean aszmic, final int appKeyIndex) {
        if (model.getBoundAppkeys().isEmpty())
            throw new IllegalArgumentException("There are no app keys bound to this model");

        final byte[] appKey = MeshParserUtils.toByteArray(model.getBoundAppKey(appKeyIndex));

        final GenericLevelGet genericLevelGet = new GenericLevelGet(node, appKey, appKeyIndex, aszmic ? 1 : 0);
        getGenericLevel(dstAddress, genericLevelGet);
    }

    @Override
    public final void getGenericLevel(@NonNull final byte[] dstAddress, @NonNull final GenericLevelGet genericLevelGet) {
        final GenericLevelGetState genericLevelGetState = new GenericLevelGetState(mContext, dstAddress, genericLevelGet, this);
        genericLevelGetState.setTransportCallbacks(mInternalTransportCallbacks);
        genericLevelGetState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = genericLevelGetState;
        genericLevelGetState.executeSend();
    }

    @Override
    public final void setGenericLevel(@NonNull final ProvisionedMeshNode node, @NonNull final MeshModel model, @NonNull final byte[] dstAddress,
                                      final boolean aszmic, final int appKeyIndex, final Integer transitionSteps,
                                      final Integer transitionResolution, final Integer delay, final int level) throws IllegalArgumentException {
        if (model.getBoundAppkeys().isEmpty())
            throw new IllegalArgumentException("There are no app keys bound to this model");

        final byte[] appKey = MeshParserUtils.toByteArray(model.getBoundAppKey(appKeyIndex));
        final GenericLevelSet genericLevelSet = new GenericLevelSet(node, appKey, appKeyIndex, transitionSteps, transitionResolution, delay, level, aszmic ? 1 : 0);
        setGenericLevel(dstAddress, genericLevelSet);
    }

    @Override
    public final void setGenericLevel(@NonNull final byte[] dstAddress, @NonNull final GenericLevelSet genericLevelSet) {
        final GenericLevelSetState genericLevelSetState = new GenericLevelSetState(mContext, dstAddress, genericLevelSet, this);
        genericLevelSetState.setTransportCallbacks(mInternalTransportCallbacks);
        genericLevelSetState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = genericLevelSetState;
        genericLevelSetState.executeSend();
    }

    @Override
    public final void setGenericLevelUnacknowledged(@NonNull final ProvisionedMeshNode node, @NonNull final MeshModel model, @NonNull final byte[] dstAddress,
                                                    final boolean aszmic, final int appKeyIndex, final Integer transitionSteps,
                                                    final Integer transitionResolution, final Integer delay, final int level) throws IllegalArgumentException {
        if (model.getBoundAppkeys().isEmpty())
            throw new IllegalArgumentException("There are no app keys bound to this model");

        final byte[] appKey = MeshParserUtils.toByteArray(model.getBoundAppKey(appKeyIndex));
        final GenericLevelSetUnacknowledged genericLevelSetUnacked = new GenericLevelSetUnacknowledged(node, appKey, appKeyIndex, transitionSteps, transitionResolution, delay, level, aszmic ? 1 : 0);
        setGenericLevelUnacknowledged(dstAddress, genericLevelSetUnacked);
    }

    @Override
    public final void setGenericLevelUnacknowledged(@NonNull final byte[] dstAddress, @NonNull final GenericLevelSetUnacknowledged genericLevelSetUnacked) {
        final GenericLevelSetUnacknowledgedState genericLevelSetUnAckedState = new GenericLevelSetUnacknowledgedState(mContext, dstAddress, genericLevelSetUnacked, this);
        genericLevelSetUnAckedState.setTransportCallbacks(mInternalTransportCallbacks);
        genericLevelSetUnAckedState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = genericLevelSetUnAckedState;
        genericLevelSetUnAckedState.executeSend();
    }

    @Override
    public void sendVendorModelUnacknowledgedMessage(@NonNull final ProvisionedMeshNode node, @NonNull final VendorModel model, @NonNull final byte[] dstAddress, final boolean aszmic, final int appKeyIndex, @NonNull final int opcode, final byte[] parameters) {
        if (model.getBoundAppkeys().isEmpty())
            throw new IllegalArgumentException("There are no app keys bound to this model");

        final byte[] appKey = MeshParserUtils.toByteArray(model.getBoundAppKey(appKeyIndex));
        final int companyIdentifier = model.getCompanyIdentifier();
        final VendorModelMessageUnacked vendorModelMessageUnacked = new VendorModelMessageUnacked(node, appKey, appKeyIndex, companyIdentifier, opcode, parameters, aszmic ? 1 : 0);
        sendVendorModelUnacknowledgedMessage(dstAddress, vendorModelMessageUnacked);
    }

    @Override
    public void sendVendorModelUnacknowledgedMessage(@NonNull final byte[] dstAddress, @NonNull final VendorModelMessageUnacked vendorModelMessageUnacked) {
        final VendorModelMessageUnackedState vendorModelMessageUnackedState = new VendorModelMessageUnackedState(mContext, dstAddress, vendorModelMessageUnacked, this);
        vendorModelMessageUnackedState.setTransportCallbacks(mInternalTransportCallbacks);
        vendorModelMessageUnackedState.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = vendorModelMessageUnackedState;
        vendorModelMessageUnackedState.executeSend();
    }

    @Override
    public final void sendVendorModelAcknowledgedMessage(@NonNull final ProvisionedMeshNode node, @NonNull final VendorModel model, @NonNull final byte[] dstAddress, final boolean aszmic, final int appKeyIndex, final int opcode, final byte[] parameters) {
        if (model.getBoundAppkeys().isEmpty())
            throw new IllegalArgumentException("There are no app keys bound to this model");

        final byte[] appKey = MeshParserUtils.toByteArray(model.getBoundAppKey(appKeyIndex));
        final int companyIdentifier = model.getCompanyIdentifier();
        final VendorModelMessageAcked vendorModelMessageAcked = new VendorModelMessageAcked(node, appKey, appKeyIndex, companyIdentifier, opcode, parameters, aszmic ? 1 : 0);
        sendVendorModelAcknowledgedMessage(dstAddress, vendorModelMessageAcked);
    }

    @Override
    public final void sendVendorModelAcknowledgedMessage(@NonNull final byte[] dstAddress, @NonNull final VendorModelMessageAcked vendorModelMessageAcked) {
        final VendorModelMessageAckedState message = new VendorModelMessageAckedState(mContext, dstAddress,vendorModelMessageAcked, this);
        message.setTransportCallbacks(mInternalTransportCallbacks);
        message.setStatusCallbacks(mStatusCallbacks);
        mMeshMessageState = message;
        message.executeSend();
    }

    @Override
    public void sendMeshMessage(@NonNull final MeshMessage meshMessage) {

    }

    @Override
    public void sendMeshMessage(@NonNull final byte[] dstAddress, @NonNull final MeshMessage meshMessage) {

    }
}
