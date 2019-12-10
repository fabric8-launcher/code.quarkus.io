import React, { useState, MouseEvent, useEffect } from "react";
import copy from 'copy-to-clipboard';
import { ClipboardCheckIcon, ClipboardIcon } from "@patternfly/react-icons";
import { useAnalytics } from '../core';
import { Tooltip } from '@patternfly/react-core';

type TooltipPosition = 'auto' | 'top' | 'bottom' | 'left' | 'right';

interface CopyToClipboardProps {
  eventId?: string;
  content: string;
  children?: React.ReactNode;
  tooltipPosition?: TooltipPosition;
  zIndex?: number;
  onClick?: (e: MouseEvent) => void;
}

export function CopyToClipboard(props: CopyToClipboardProps) {
  const [active, setActive] = useState(false);
  const [copied, setCopied] = useState(false);
  const [copiedText, setCopiedText] = useState(false);
  const [timeoutRef1, setTimeoutRef1] = useState();
  const [timeoutRef2, setTimeoutRef2] = useState();
  const analytics = useAnalytics();
  
  useEffect(() => {
    return () => {
      clearTimeout(timeoutRef1);
      clearTimeout(timeoutRef2);
    }
  }, [timeoutRef1, timeoutRef2]);

  const copyToClipboard = (e: MouseEvent) => {
    e.stopPropagation();
    props.onClick && props.onClick(e)
    copy(props.content);
    if (props.eventId && !copied) {
      analytics && analytics.event('Copy-To-Clipboard', props.eventId, props.content);
    }
    setCopied(true);
    setCopiedText(true);
    setTimeoutRef1(setTimeout(() => setCopiedText(false), 2000));
    setTimeoutRef2(setTimeout(() => setCopied(false), 1500));
  }
  const tooltip = copiedText ? <h3>Successfuly copied to clipboard!</h3> : <span>Copy to clipboard: <br /><code>{props.content}</code></span>;
  return (
    <Tooltip position={props.tooltipPosition} maxWidth="650px" content={tooltip} entryDelay={0} exitDelay={0} trigger="manual" isVisible={copied || active} zIndex={props.zIndex || 100}>
      <div
        onMouseEnter={() => setActive(true)}
        onMouseLeave={() => setActive(false)}
        onClick={copyToClipboard}
        className="copy-to-clipboard"
        style={{ cursor: 'pointer' }}
      >
        {active || copied ? <ClipboardCheckIcon /> : <ClipboardIcon />}{props.children}
      </div>
    </Tooltip>
  )

}
