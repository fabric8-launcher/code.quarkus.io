import { ExternalLink, useAnalytics } from '../core';
import { Button, Modal, TextContent } from '@patternfly/react-core';
import React from 'react';
import { CopyToClipboard } from './copy-to-clipboard';

interface NextStepsProps {
  downloadLink?: string;
  onClose?(reset?: boolean): void;
  buildTool: string
}

export function NextSteps(props: NextStepsProps) {
  const analytics = useAnalytics();
  const baseEvent = ["UX", "Post-Generate Popup Action"];
  const close = (reset?: boolean) => {
    analytics.event(baseEvent[0], baseEvent[1], reset ? "Start new" : "Close");
    props.onClose && props.onClose(reset);
  };
  const onClickDownload = () => {
    analytics.event(baseEvent[0], baseEvent[1], 'Click "Download the zip" link')
  };
  const onClickGuides = () => {
    analytics.event(baseEvent[0], baseEvent[1], 'Click "guides" link')
  };
  const devModeEvent = [...baseEvent, 'Copy "Dev mode" command'];
  return (
    <Modal
      title="Your Quarkus Application is Ready!"
      isSmall={true}
      className="next-steps-modal"
      onClose={() => close(false)}
      isOpen={true}
      aria-label="Your new Quarkus app has been generated"
      actions={[
        <Button key="go-back" variant="secondary" aria-label="Close this popup" onClick={() => close(false)}>
          Close
        </Button>,
        <Button key="start-new" variant="secondary" aria-label="Start a new application" onClick={() => close()}>
          Start a new application
        </Button>
      ]}
    >
      <TextContent>
        <p>Your download should start shortly. If it doesn't, please use the direct link:</p>
        <Button component="a" href={props.downloadLink as string} aria-label="Download link" className="download-button" onClick={onClickDownload}>Download the zip</Button>
        <h1>What's next?</h1>
        <div>
          Unzip the project and start playing with Quarkus by running:

          {props.buildTool === 'MAVEN' &&
          <code>$ ./mvnw compile quarkus:dev <CopyToClipboard zIndex={5000} tooltipPosition="left" event={devModeEvent} content="./mvnw compile quarkus:dev"/></code>
          }

          {props.buildTool === 'GRADLE' &&
          <code>$ ./gradlew quarkusDev <CopyToClipboard zIndex={5000} tooltipPosition="left" event={devModeEvent} content="./gradlew quarkusDev"/></code>
          }
          Follow the <ExternalLink href="https://quarkus.io/guides/" aria-label="Start playing with Quarkus" onClick={onClickGuides}>guides</ExternalLink>  for your next steps!
        </div>
      </TextContent>
    </Modal>
  );
}
