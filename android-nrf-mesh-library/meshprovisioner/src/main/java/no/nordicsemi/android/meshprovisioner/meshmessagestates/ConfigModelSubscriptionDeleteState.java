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

package no.nordicsemi.android.meshprovisioner.meshmessagestates;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import no.nordicsemi.android.meshprovisioner.InternalMeshMsgHandlerCallbacks;
import no.nordicsemi.android.meshprovisioner.messages.ConfigModelSubscriptionDelete;
import no.nordicsemi.android.meshprovisioner.messages.ConfigModelSubscriptionStatus;
import no.nordicsemi.android.meshprovisioner.messagetypes.AccessMessage;
import no.nordicsemi.android.meshprovisioner.messagetypes.ControlMessage;
import no.nordicsemi.android.meshprovisioner.messagetypes.Message;
import no.nordicsemi.android.meshprovisioner.opcodes.ConfigMessageOpCodes;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

/**
 * This class handles subscribing a model to subscription address.
 */
public final class ConfigModelSubscriptionDeleteState extends ConfigMessageState {

    private static final String TAG = ConfigModelSubscriptionDeleteState.class.getSimpleName();
    private final ConfigModelSubscriptionDelete mConfigModelSubscriptionDelete;

    public ConfigModelSubscriptionDeleteState(@NonNull final Context context,
                                              @NonNull final ConfigModelSubscriptionDelete configModelSubscriptionDelete,
                                              @NonNull final InternalMeshMsgHandlerCallbacks callbacks) {
        super(context, configModelSubscriptionDelete.getMeshNode(), callbacks);
        this.mConfigModelSubscriptionDelete = configModelSubscriptionDelete;
        createAccessMessage();
    }

    @Override
    public MessageState getState() {
        return MessageState.CONFIG_MODEL_SUBSCRIPTION_DELETE_STATE;
    }

    @Override
    protected boolean parseMeshPdu(final byte[] pdu) {
        final Message message = mMeshTransport.parsePdu(mSrc, pdu);
        if (message != null) {
            if (message instanceof AccessMessage) {
                final ConfigModelSubscriptionStatus configModelSubscriptionAdd = new ConfigModelSubscriptionStatus(mNode, (AccessMessage) message);
                //TODO Config model subscription status
                mInternalTransportCallbacks.updateMeshNode(mNode);
            } else {
                parseControlMessage((ControlMessage) message, mPayloads.size());
                return true;
            }
        } else {
            Log.v(TAG, "Message reassembly may not be complete yet");
        }
        return false;
    }

    /**
     * Creates the access message to be sent to the node
     */
    private void createAccessMessage() {
        final byte[] key = mNode.getDeviceKey();
        final int akf = mConfigModelSubscriptionDelete.getAkf();
        final int aid = mConfigModelSubscriptionDelete.getAid();
        final int aszmic = mConfigModelSubscriptionDelete.getAszmic();
        final int opCode = mConfigModelSubscriptionDelete.getOpCode();
        final byte[] parameters = mConfigModelSubscriptionDelete.getParameters();
        message = mMeshTransport.createMeshMessage(mNode, mSrc, key, akf, aid, aszmic, opCode, parameters);
        mPayloads.putAll(message.getNetworkPdu());
    }

    @Override
    public final void executeSend() {
        Log.v(TAG, "Sending config model subscription delete");
        super.executeSend();

        if (!mPayloads.isEmpty()) {
            if (mMeshStatusCallbacks != null)
                mMeshStatusCallbacks.onSubscriptionDeleteSent(mNode);
        }
    }

    public void parseData(final byte[] pdu) {
        parseMeshPdu(pdu);
    }

    @Override
    public void sendSegmentAcknowledgementMessage(final ControlMessage controlMessage) {
        final ControlMessage message = mMeshTransport.createSegmentBlockAcknowledgementMessage(controlMessage);
        Log.v(TAG, "Sending acknowledgement: " + MeshParserUtils.bytesToHex(message.getNetworkPdu().get(0), false));
        mInternalTransportCallbacks.sendPdu(mNode, message.getNetworkPdu().get(0));
        mMeshStatusCallbacks.onBlockAcknowledgementSent(mNode);
    }

    /**
     * Returns the source address of the message i.e. where it originated from
     *
     * @return source address
     */
    public byte[] getSrc() {
        return mSrc;
    }
}
